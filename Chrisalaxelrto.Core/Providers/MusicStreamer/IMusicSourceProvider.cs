using Chrisalaxelrto.Core.Models.MusicStreamer;

namespace Chrisalaxelrto.Core.Providers.MusicStreamer;
public interface IMusicSourceProvider
{
    string SourceName { get; }
    bool CanHandle(string url);
    Task<AudioStream?> GetAudioStreamAsync(string url, AudioQuality quality = AudioQuality.VeryHigh);
    Task<Stream> GetAudioDataAsync(string streamUrl);
    Task<IEnumerable<AudioStream>> SearchAsync(string query, int maxResults = 10);
}