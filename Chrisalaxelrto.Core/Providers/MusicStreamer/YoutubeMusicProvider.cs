namespace Chrisalaxelrto.Core.Providers.MusicStreamer;

using Chrisalaxelrto.Core.Models.MusicStreamer;
using Microsoft.Extensions.Logging;
using YoutubeExplode;
using YoutubeExplode.Common;
using YoutubeExplode.Videos.Streams;

public class YouTubeMusicProvider : IMusicSourceProvider
{
    private readonly YoutubeClient _youtubeClient;
    private readonly HttpClient _httpClient;
    private readonly ILogger<YouTubeMusicProvider> _logger;

    public string SourceName => "YouTube";

    public YouTubeMusicProvider(HttpClient httpClient, ILogger<YouTubeMusicProvider> logger)
    {
        _youtubeClient = new YoutubeClient(httpClient);
        _httpClient = httpClient;
        _logger = logger;
    }

    public bool CanHandle(string url)
    {
        return url.Contains("youtube.com/watch") || 
               url.Contains("youtu.be/") || 
               url.Contains("music.youtube.com");
    }

    public async Task<AudioStream?> GetAudioStreamAsync(string url, AudioQuality quality = AudioQuality.Medium)
    {
        try
        {
            var video = await _youtubeClient.Videos.GetAsync(url);
            var streamManifest = await _youtubeClient.Videos.Streams.GetManifestAsync(url);
            
            var audioStreamInfo = GetBestAudioStream(streamManifest, quality);
            if (audioStreamInfo == null)
            {
                _logger.LogWarning("No suitable audio stream found for video: {VideoId}", video.Id);
                return null;
            }

            return new AudioStream
            {
                Id = video.Id.Value,
                Title = video.Title,
                Artist = video.Author.ChannelTitle,
                Duration = video.Duration ?? TimeSpan.Zero,
                ThumbnailUrl = video.Thumbnails.GetWithHighestResolution()?.Url ?? string.Empty,
                StreamUrl = audioStreamInfo.Url,
                Quality = MapBitrateToQuality(audioStreamInfo.Bitrate),
                Source = SourceName
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting audio stream for URL: {Url}", url);
            return null;
        }
    }

    public async Task<Stream> GetAudioDataAsync(string streamUrl)
    {
        var response = await _httpClient.GetAsync(streamUrl, HttpCompletionOption.ResponseHeadersRead);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStreamAsync();
    }

    public async Task<IEnumerable<AudioStream>> SearchAsync(string query, int maxResults = 10)
    {
        try
        {
            var searchResults = new List<AudioStream>();
            var videos = _youtubeClient.Search.GetVideosAsync(query).Take(maxResults);
            
            await foreach (var video in videos)
            {
                searchResults.Add(new AudioStream
                {
                    Id = video.Id.Value,
                    Title = video.Title,
                    Artist = video.Author.ChannelTitle,
                    Duration = video.Duration ?? TimeSpan.Zero,
                    ThumbnailUrl = video.Thumbnails.GetWithHighestResolution()?.Url ?? string.Empty,
                    StreamUrl = string.Empty, // Will be populated when requested
                    AudioSourceUrl = video.Url,
                    Quality = AudioQuality.Medium,
                    Source = SourceName
                });
            }
            
            return searchResults;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error searching for: {Query}", query);
            return Enumerable.Empty<AudioStream>();
        }
    }

    private static IStreamInfo? GetBestAudioStream(StreamManifest manifest, AudioQuality quality)
    {
        var targetBitrate = (int)quality;
        
        return manifest.GetAudioOnlyStreams()
            .Where(s => s.Container == Container.Mp4 || s.Container == Container.WebM)
            .OrderBy(s => Math.Abs(s.Bitrate.KiloBitsPerSecond - targetBitrate))
            .FirstOrDefault();
    }

    private static AudioQuality MapBitrateToQuality(Bitrate bitrate)
    {
        var kbps = bitrate.KiloBitsPerSecond;
        return kbps switch
        {
            <= 80 => AudioQuality.Low,
            <= 160 => AudioQuality.Medium,
            <= 250 => AudioQuality.High,
            _ => AudioQuality.VeryHigh
        };
    }
}