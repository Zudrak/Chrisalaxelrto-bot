using Chrisalaxelrto.TrackStreamer.Models;

namespace Chrisalaxelrto.TrackStreamer.Providers;
public interface ITrackSourceProvider
{
    TrackSource Source { get; }
    bool CanHandle(Uri url);
    Task<SourceMetadata?> GetSourceMetadata(Uri url, AudioQuality quality = AudioQuality.VeryHigh);

    Task<TrackMetadata?> GetTrackMetadata(Uri url);
    Task<IEnumerable<TrackMetadata>> SearchAsync(string query, int maxResults = 10);
}