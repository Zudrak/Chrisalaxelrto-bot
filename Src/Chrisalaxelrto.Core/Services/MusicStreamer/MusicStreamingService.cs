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

    public async Task<StreamResponse> GetStreamAsync(StreamRequest request)
    {
        try
        {
            var provider = GetProviderForUrl(request.Url);
            if (provider == null)
            {
                return new StreamResponse
                {
                    Success = false,
                    Message = "No provider found for the given URL"
                };
            }

            var cacheKey = $"stream_{request.Url}_{request.Quality}";
            if (_cache.TryGetValue(cacheKey, out AudioStream? cachedStream))
            {
                return new StreamResponse
                {
                    Success = true,
                    Stream = cachedStream
                };
            }

            var stream = await provider.GetAudioStreamAsync(request.Url, request.Quality);
            if (stream == null)
            {
                return new StreamResponse
                {
                    Success = false,
                    Message = "Failed to extract audio stream"
                };
            }

            _cache.Set(cacheKey, stream, TimeSpan.FromMinutes(30));

            return new StreamResponse
            {
                Success = true,
                Stream = stream
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing stream request for URL: {Url}", request.Url);
            return new StreamResponse
            {
                Success = false,
                Message = "Internal server error"
            };
        }
    }

    public async Task<Stream> GetAudioDataAsync(string url)
    {
        var provider = GetProviderForUrl(url);
        if (provider == null)
        {
            throw new InvalidOperationException("No provider found for the given URL");
        }

        var stream = await provider.GetAudioStreamAsync(url);
        if (stream?.StreamUrl == null)
        {
            throw new InvalidOperationException("Failed to get stream URL");
        }

        return await provider.GetAudioDataAsync(stream.StreamUrl);
    }

    public async Task<IEnumerable<AudioStream>> SearchAsync(string query, string? source = null, int maxResults = 10)
    {
        var providers = string.IsNullOrEmpty(source) 
            ? _providers 
            : _providers.Where(p => p.SourceName.Equals(source, StringComparison.OrdinalIgnoreCase));

        var tasks = providers.Select(p => p.SearchAsync(query, maxResults));
        var results = await Task.WhenAll(tasks);
        
        return results.SelectMany(r => r).Take(maxResults);
    }

    private IMusicSourceProvider? GetProviderForUrl(string url)
    {
        return _providers.FirstOrDefault(p => p.CanHandle(url));
    }

    public IEnumerable<string> GetAvailableSources()
    {
        return _providers.Select(p => p.SourceName);
    }
}