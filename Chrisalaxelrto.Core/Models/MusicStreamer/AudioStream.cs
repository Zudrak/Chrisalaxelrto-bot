namespace Chrisalaxelrto.Core.Models.MusicStreamer;

public class AudioStream
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Artist { get; set; } = string.Empty;
    public TimeSpan Duration { get; set; }
    public string ThumbnailUrl { get; set; } = string.Empty;
    public string StreamUrl { get; set; } = string.Empty;
    public string AudioSourceUrl { get; set; } = string.Empty;
    public AudioQuality Quality { get; set; }
    public string Source { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}