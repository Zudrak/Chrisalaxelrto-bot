﻿namespace Chrisalaxelrto.Core.Models.MusicStreamer
{
    using System.Net.Http.Headers;

    public class MusicResponse
    {
        public required TrackMetadata TrackMetadata { get; set; }
        public required string StreamUrl { get; set; }
        public required MediaTypeHeaderValue ContentType { get; set; }
    }
}