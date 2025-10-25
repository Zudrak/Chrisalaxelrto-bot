using Chrisalaxelrto.TrackStreamer.Providers;

namespace Chrisalaxelrto.TrackStreamer.Models;

public class TrackMetadata
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Artist { get; set; } = string.Empty;
    public TimeSpan Duration { get; set; }
    public string ThumbnailUrl { get; set; } = string.Empty;
    required public Uri Uri { get; set; }
    public TrackSource Source { get; set; }
    public DateTime CreatedAt { get; set; }
}