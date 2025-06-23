namespace Chrisalaxelrto.Core.Models.MusicStreamer;

public class StreamRequest
{
    public string Url { get; set; } = string.Empty;
    public AudioQuality Quality { get; set; } = AudioQuality.Medium;
    public string? StartTime { get; set; }
    public string? EndTime { get; set; }
}