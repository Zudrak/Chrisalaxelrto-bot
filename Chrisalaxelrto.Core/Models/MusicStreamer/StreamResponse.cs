namespace Chrisalaxelrto.Core.Models.MusicStreamer;

public class StreamResponse
{
    public bool Success { get; set; }
    public string? Message { get; set; }
    public AudioStream? Stream { get; set; }
}