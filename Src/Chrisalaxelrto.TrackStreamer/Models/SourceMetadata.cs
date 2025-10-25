namespace Chrisalaxelrto.TrackStreamer.Models
{
    using System.Net.Http.Headers;

    public class SourceMetadata
    {
        public required TrackMetadata TrackMetadata { get; set; }
        public required string StreamUrl { get; set; }
        public required MediaTypeHeaderValue ContentType { get; set; }
    }
}