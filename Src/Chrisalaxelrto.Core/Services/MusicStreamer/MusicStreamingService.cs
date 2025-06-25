using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Logging;

namespace Chrisalaxelrto.Core.Services;

public class MusicStreamingService
{
    private readonly IEnumerable<IMusicSourceProvider> _providers;
    private readonly IMemoryCache _cache;
    private readonly ILogger<MusicStreamingService> _logger;

    public MusicStreamingService(
        IEnumerable<IMusicSourceProvider> providers,
        IMemoryCache cache,
        ILogger<MusicStreamingService> logger)
    {
        _providers = providers;
        _cache = cache;
        _logger = logger;
    }

    public async Task<MusicResponse?> GetMusicResponse(string musicQuery, MusicSource source = MusicSource.YouTube)
    {
        try
        {
            if (string.IsNullOrEmpty(musicQuery))
            {
                throw new ArgumentException("Music query cannot be null or empty", nameof(musicQuery));
            }

            TrackMetadata? metadata = null;
            Uri? musicUri = null;
            if (!Uri.TryCreate(musicQuery, UriKind.Absolute, out musicUri))
            {
                var searchResults = await SearchAsync(musicQuery, source);
                if (!searchResults.Any())
                {
                    _logger.LogWarning("No results found for query: {Query}", musicQuery);
                    return null;
                }
                musicUri = searchResults.First().Uri;
                metadata = searchResults.FirstOrDefault();
            }

            var provider = GetProviderForUrl(musicUri);

            if (provider == null)
            {
                return null;
            }

            if (metadata == null)
            {
                metadata = await provider.GetTrackMetadata(musicUri);
                if (metadata == null)
                {
                    _logger.LogWarning("No metadata found for URL: {Url}", musicUri);
                    return null;
                }
            }

            return await provider.GetMusicResponseAsync(metadata.Uri);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing stream request for query: {musicQuery}", musicQuery);
            return null;
        }
    }

    public async Task<IEnumerable<TrackMetadata>> SearchAsync(string query, MusicSource? source = null, int maxResults = 10)
    {
        var providers = source == null ? _providers : _providers.Where(p => p.Source == source);

        var tasks = providers.Select(p => p.SearchAsync(query, maxResults));
        var results = await Task.WhenAll(tasks);
        
        return results.SelectMany(r => r).Take(maxResults);
    }

    private IMusicSourceProvider? GetProviderForUrl(Uri url)
    {
        return _providers.FirstOrDefault(p => p.CanHandle(url));
    }

    public IEnumerable<MusicSource> GetAvailableSources()
    {
        return _providers.Select(p => p.Source);
    }
}