using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Chrisalaxelrto.Core.Services;
using Microsoft.AspNetCore.Mvc;
using System.Net;

namespace Chrisalaxelrto.MusicStreamer.Controllers;

[ApiController]
[Route("music")]
public class MusicController : ControllerBase
{
    private readonly MusicStreamingService _musicService;
    private readonly ILogger<MusicController> _logger;
    private readonly IHttpClientFactory _httpClientFactory;

    public MusicController(MusicStreamingService musicService, ILogger<MusicController> logger, IHttpClientFactory httpClientFactory)
    {
        _musicService = musicService;
        _logger = logger;
        _httpClientFactory = httpClientFactory;
    }

    [HttpGet("stream")]
    public async Task<IActionResult> StreamAudio([FromQuery] string musicQuery, [FromQuery] AudioQuality quality = AudioQuality.VeryHigh)
    {
        try
        {
            if (string.IsNullOrEmpty(musicQuery))
            {
                return BadRequest("Music query is required");
            }

            var musicResponse = await _musicService.GetMusicResponse(musicQuery);

            if (musicResponse == null)
            {
                return BadRequest("Failed to get stream information");
            }

            // Ensure ContentType.MediaType is not null before accessing it
            var mediaType = musicResponse.ContentType?.MediaType;
            if (string.IsNullOrEmpty(mediaType))
            {
                return BadRequest("Invalid media type in response");
            }

            // Serialize metadata to JSON and add as header
            var metadataJson = System.Text.Json.JsonSerializer.Serialize(musicResponse.TrackMetadata);
            Response.Headers.Append("X-Track-Metadata", Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes(metadataJson)));

            // Create HttpClient with proxy configuration for YouTube streaming
            var httpClientHandler = new HttpClientHandler
            {
                UseCookies = false,
                Proxy = new WebProxy("axel-pc:3128") 
                {
                    BypassProxyOnLocal = false
                }
            };

            var httpClient = new HttpClient(httpClientHandler)
            {
                Timeout = TimeSpan.FromSeconds(300) // Longer timeout for streaming
            };

            // Get the stream from YouTube with ResponseHeadersRead to start streaming immediately
            var response = await httpClient.GetAsync(musicResponse.StreamUrl, HttpCompletionOption.ResponseHeadersRead);
            
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogError("Failed to get stream from YouTube. Status: {StatusCode}", response.StatusCode);
                return StatusCode(500, "Failed to retrieve audio stream");
            }

            // Set response headers for streaming
            Response.Headers.ContentType = mediaType;
            if (response.Content.Headers.ContentLength.HasValue)
            {
                Response.Headers.ContentLength = response.Content.Headers.ContentLength.Value;
            }

            // Enable range processing for seeking support
            Response.Headers.AcceptRanges = "bytes";

            // Stream the content directly from YouTube to the client
            var stream = await response.Content.ReadAsStreamAsync();
            
            _logger.LogInformation("Started streaming audio for '{Title}' by '{Artist}'", 
                musicResponse.TrackMetadata.Title, musicResponse.TrackMetadata.Artist);

            return new FileStreamResult(stream, mediaType)
            {
                EnableRangeProcessing = true,
                FileDownloadName = $"{musicResponse.TrackMetadata.Title}.{mediaType.Split('/')[1]}"
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error streaming audio");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("search")]
    public async Task<ActionResult<IEnumerable<TrackMetadata>>> Search(
        [FromQuery] string query,
        [FromQuery] MusicSource? source = null,
        [FromQuery] int maxResults = 10)
    {
        if (string.IsNullOrEmpty(query))
        {
            return BadRequest("Query is required");
        }

        var results = await _musicService.SearchAsync(query, source, maxResults);
        return Ok(results);
    }
}