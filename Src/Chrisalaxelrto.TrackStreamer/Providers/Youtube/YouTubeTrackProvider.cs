namespace Chrisalaxelrto.TrackStreamer.Providers.Youtube;

using System.Net.Http.Headers;
using Chrisalaxelrto.TrackStreamer.Models;
using Microsoft.AspNetCore.StaticFiles;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using YoutubeExplode;
using YoutubeExplode.Common;
using YoutubeExplode.Videos;
using YoutubeExplode.Videos.Streams;

public class YouTubeTrackProvider : ITrackSourceProvider
{
    private readonly YoutubeClient youtubeClient;
    private readonly ILogger<YouTubeTrackProvider> logger;

    public TrackSource Source => TrackSource.YouTube;

    public YouTubeTrackProvider(ILogger<YouTubeTrackProvider> logger, IConfiguration configuration)
    {
        // var base64Cookies = configuration["YoutubeCookies"];

        // if (string.IsNullOrEmpty(base64Cookies))
        // {
        //     throw new ArgumentException("Youtube cookies are not configured. Please set the \"YoutubeCookies\" variable in appsettings.json or environment.");
        // }
        // var cookies = CookieParser.ParseCookies(base64Cookies);

        HttpClientHandler httpClientHandler = new HttpClientHandler
        {
            UseCookies = true,
        };
        
        var httpClient = new HttpClient(httpClientHandler)
        { 
            Timeout = TimeSpan.FromSeconds(200)
        };
        youtubeClient = new YoutubeClient(httpClient);
        this.logger = logger;
        logger.LogInformation("YouTubeMusicProvider initialized.");
    }

    public bool CanHandle(Uri url)
    {
        var stringUrl = url.ToString();
        return stringUrl.Contains("youtube.com/watch") ||
               stringUrl.Contains("youtu.be/") ||
               stringUrl.Contains("music.youtube.com");
    }

    public async Task<SourceMetadata?> GetSourceMetadata(Uri url, AudioQuality quality = AudioQuality.VeryHigh)
    {
        try
        {
            logger.LogInformation("Getting music response for URL: {Url}", url);

            var video = await youtubeClient.Videos.GetAsync(url.ToString());
            var streamManifest = await youtubeClient.Videos.Streams.GetManifestAsync(url.ToString());
            
            var audioStreamInfo = GetBestAudioStream(streamManifest, quality);
            if (audioStreamInfo == null)
            {
                logger.LogWarning("No suitable audio stream found for video: {VideoId}", video.Id);
                return null;
            }

            var metadata = ConvertVideoSearchResultToTrackMetadata(video);
            if (metadata == null)
            {
                logger.LogWarning("Failed to convert video metadata for video: {VideoId}", video.Id);
                return null;
            } 

            if (!new FileExtensionContentTypeProvider().TryGetContentType($".{audioStreamInfo.Container.Name}", out var contentType))
            {
                return null;
            }

            return new SourceMetadata
            {
                ContentType = new MediaTypeHeaderValue(contentType),
                StreamUrl = audioStreamInfo.Url,
                TrackMetadata = metadata
            };
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error getting audio stream for URL: {Url}", url);
            return null;
        }
    }
    
    public async Task<TrackMetadata?> GetTrackMetadata(Uri url)
    {
        try
        {
            logger.LogInformation("Getting track metadata for URL: {Url}", url);

            var video = await youtubeClient.Videos.GetAsync(url.ToString());
            if (video == null)
            {
                return null;
            }
            return ConvertVideoSearchResultToTrackMetadata(video);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error getting track metadata for URL: {Url}", url);
            return null;
        }
    }

    public async Task<IEnumerable<TrackMetadata>> SearchAsync(string query, int maxResults = 10)
    {
        try
        {
            logger.LogInformation("Searching for: {query}", query);

            var searchResults = new List<TrackMetadata>();
            var videos = await youtubeClient.Search.GetVideosAsync(query).Take(maxResults);
            
            foreach (var video in videos)
            {
                searchResults.Add(ConvertVideoSearchResultToTrackMetadata(video));
            }
            
            return searchResults;
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error searching for: {Query}", query);
            return Enumerable.Empty<TrackMetadata>();
        }
    }
    
    private TrackMetadata ConvertVideoSearchResultToTrackMetadata(IVideo video)
    {
        return new TrackMetadata
        {
            Id = video.Id.Value,
            Title = video.Title,
            Artist = video.Author.ChannelTitle,
            Duration = video.Duration ?? TimeSpan.Zero,
            ThumbnailUrl = video.Thumbnails.GetWithHighestResolution()?.Url ?? string.Empty,
            Uri = new Uri(video.Url),
            Source = Source
        };
    }

    private IStreamInfo? GetBestAudioStream(StreamManifest manifest, AudioQuality quality)
    {
        var targetBitrate = (int)quality;
        
        return manifest.GetAudioOnlyStreams()
            .Where(s => s.Container == Container.Mp4 || s.Container == Container.WebM)
            .OrderBy(s => Math.Abs(s.Bitrate.KiloBitsPerSecond - targetBitrate))
            .FirstOrDefault();
    }
}