using Chrisalaxelrto.TrackStreamer.Models;
using Chrisalaxelrto.TrackStreamer.Providers;
using Microsoft.Extensions.Logging;

namespace Chrisalaxelrto.TrackStreamer.Services;

public class TrackMetadataService
{
    private readonly IEnumerable<ITrackSourceProvider> providers;
    private readonly ILogger<TrackMetadataService> logger;

    public TrackMetadataService(
        IEnumerable<ITrackSourceProvider> providers,
        ILogger<TrackMetadataService> logger)
    {
        this.providers = providers;
        this.logger = logger;
    }

    public async Task<SourceMetadata?> GetSourceMetadata(string musicQuery, TrackSource source = TrackSource.YouTube)
    {
        try
        {
            if (string.IsNullOrEmpty(musicQuery))
            {
                throw new ArgumentException("Track query cannot be null or empty", nameof(musicQuery));
            }

            TrackMetadata? metadata = null;
            Uri? musicUri = null;
            if (!Uri.TryCreate(musicQuery, UriKind.Absolute, out musicUri))
            {
                var searchResults = await SearchAsync(musicQuery, source);
                if (!searchResults.Any())
                {
                    logger.LogWarning("No results found for query: {Query}", musicQuery);
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
                    logger.LogWarning("No metadata found for URL: {Url}", musicUri);
                    return null;
                }
            }

            return await provider.GetSourceMetadata(metadata.Uri);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error processing stream request for query: {musicQuery}", musicQuery);
            return null;
        }
    }

    public async Task<IEnumerable<TrackMetadata>> SearchAsync(string query, TrackSource? source = null, int maxResults = 10)
    {
        var providers = source == null ? this.providers : this.providers.Where(p => p.Source == source);

        var tasks = providers.Select(p => p.SearchAsync(query, maxResults));
        var results = await Task.WhenAll(tasks);
        
        return results.SelectMany(r => r).Take(maxResults);
    }

    private ITrackSourceProvider? GetProviderForUrl(Uri url)
    {
        return providers.FirstOrDefault(p => p.CanHandle(url));
    }

    public IEnumerable<TrackSource> GetAvailableSources()
    {
        return providers.Select(p => p.Source);
    }
}