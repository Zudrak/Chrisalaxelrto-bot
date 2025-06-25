using Chrisalaxelrto.Core.Models.MusicStreamer;

namespace Chrisalaxelrto.Core.Providers.MusicStreamer;
public interface IMusicSourceProvider
{
    MusicSource Source { get; }
    bool CanHandle(Uri url);
    Task<MusicResponse?> GetMusicResponseAsync(Uri url, AudioQuality quality = AudioQuality.VeryHigh);

    Task<TrackMetadata?> GetTrackMetadata(Uri url);
    Task<IEnumerable<TrackMetadata>> SearchAsync(string query, int maxResults = 10);
}