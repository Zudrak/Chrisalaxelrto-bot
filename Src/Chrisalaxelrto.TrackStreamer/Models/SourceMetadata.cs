namespace Chrisalaxelrto.TrackStreamer.Models
{
    using System.Diagnostics;
    using System.Net.Http.Headers;

    public class SourceMetadata
    {
        public object? ProviderSpecificMetadata { get; set; }
        public required TrackMetadata TrackMetadata { get; set; }
        public required Uri StreamUrl { get; set; }
        public required MediaTypeHeaderValue ContentType { get; set; }
    }
}