namespace Chrisalaxelrto.MusicStreamer.Providers.Youtube;

using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Microsoft.AspNetCore.StaticFiles;
using Microsoft.Extensions.Logging;
using System.Net.Http.Headers;
using YoutubeExplode;
using YoutubeExplode.Common;
using YoutubeExplode.Videos;
using YoutubeExplode.Videos.Streams;

public class YouTubeMusicProvider : IMusicSourceProvider
{
    private readonly YoutubeClient _youtubeClient;

    private readonly ILogger<YouTubeMusicProvider> _logger;

    public MusicSource Source => MusicSource.YouTube;

    public YouTubeMusicProvider(HttpClient httpClient, ILogger<YouTubeMusicProvider> logger, IConfiguration configuration)
    {
        _youtubeClient = new YoutubeClient(httpClient);
        _logger = logger;
    }

    public bool CanHandle(Uri url)
    {
        var stringUrl = url.ToString();
        return stringUrl.Contains("youtube.com/watch") ||
               stringUrl.Contains("youtu.be/") ||
               stringUrl.Contains("music.youtube.com");
    }

    public async Task<MusicResponse?> GetMusicResponseAsync(Uri url, AudioQuality quality = AudioQuality.VeryHigh)
    {
        try
        {
            var video = await _youtubeClient.Videos.GetAsync(url.ToString());
            var streamManifest = await _youtubeClient.Videos.Streams.GetManifestAsync(url.ToString());
            
            var audioStreamInfo = GetBestAudioStream(streamManifest, quality);
            if (audioStreamInfo == null)
            {
                _logger.LogWarning("No suitable audio stream found for video: {VideoId}", video.Id);
                return null;
            }
            var stream = await _youtubeClient.Videos.Streams.GetAsync(audioStreamInfo);
            if (stream == null)
            {
                _logger.LogWarning("Failed to get audio stream for video: {VideoId}", video.Id);
                return null;
            }

            var metadata = ConvertVideoSearchResultToTrackMetadata(video);
            if (metadata == null)
            {
                _logger.LogWarning("Failed to convert video metadata for video: {VideoId}", video.Id);
                return null;
            } 

            if (!new FileExtensionContentTypeProvider().TryGetContentType($".{audioStreamInfo.Container.Name}", out var contentType))
            {
                return null;
            }

            return new MusicResponse
            {
                ContentType = new MediaTypeHeaderValue(contentType),
                Stream = stream,
                TrackMetadata = metadata
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting audio stream for URL: {Url}", url);
            return null;
        }
    }
    public async Task<TrackMetadata?> GetTrackMetadata(Uri url)
    {
        try
        {
            var video = await _youtubeClient.Videos.GetAsync(url.ToString());
            if (video == null)
            {
                return null;
            }
            return ConvertVideoSearchResultToTrackMetadata(video);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting track metadata for URL: {Url}", url);
            return null;
        }
    }

    public async Task<IEnumerable<TrackMetadata>> SearchAsync(string query, int maxResults = 10)
    {
        try
        {
            var searchResults = new List<TrackMetadata>();
            var videos = await _youtubeClient.Search.GetVideosAsync(query).Take(maxResults);
            
            foreach (var video in videos)
            {
                searchResults.Add(ConvertVideoSearchResultToTrackMetadata(video));
            }
            
            return searchResults;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error searching for: {Query}", query);
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