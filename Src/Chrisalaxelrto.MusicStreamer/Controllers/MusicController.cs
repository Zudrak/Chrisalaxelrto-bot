using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Chrisalaxelrto.Core.Services;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;
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
        var totalStopwatch = Stopwatch.StartNew();
        var stepStopwatch = Stopwatch.StartNew();
        
        _logger.LogInformation("🎵 Starting stream request for query: '{Query}' with quality: {Quality}", musicQuery, quality);

        try
        {
            if (string.IsNullOrEmpty(musicQuery))
            {
                return BadRequest("Music query is required");
            }

            // Step 1: Get music response (metadata + stream URL)
            stepStopwatch.Restart();
            var musicResponse = await _musicService.GetMusicResponse(musicQuery);
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 1 - Got music response in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

            if (musicResponse == null)
            {
                _logger.LogWarning("❌ No music response found for query: '{Query}'", musicQuery);
                return BadRequest("Failed to get stream information");
            }

            // Step 2: Validate content type
            stepStopwatch.Restart();
            var mediaType = musicResponse.ContentType?.MediaType;
            if (string.IsNullOrEmpty(mediaType))
            {
                return BadRequest("Invalid media type in response");
            }
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 2 - Validated content type '{MediaType}' in {ElapsedMs}ms", mediaType, stepStopwatch.ElapsedMilliseconds);

            // Step 3: Prepare metadata header
            stepStopwatch.Restart();
            var metadataJson = System.Text.Json.JsonSerializer.Serialize(musicResponse.TrackMetadata);
            Response.Headers.Append("X-Track-Metadata", Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes(metadataJson)));
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 3 - Prepared metadata header in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

            // Step 4: Create HttpClient with proxy
            stepStopwatch.Restart();
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
                Timeout = TimeSpan.FromSeconds(300)
            };
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 4 - Created HttpClient with proxy in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

            // Step 5: Connect to YouTube stream
            stepStopwatch.Restart();
            _logger.LogInformation("🔗 Connecting to YouTube stream URL: {StreamUrl}", musicResponse.StreamUrl);
            var response = await httpClient.GetAsync(musicResponse.StreamUrl, HttpCompletionOption.ResponseHeadersRead);
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 5 - Connected to YouTube stream in {ElapsedMs}ms, Status: {StatusCode}", 
                stepStopwatch.ElapsedMilliseconds, response.StatusCode);
            
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogError("❌ Failed to get stream from YouTube. Status: {StatusCode}", response.StatusCode);
                return StatusCode(500, "Failed to retrieve audio stream");
            }

            // Step 6: Set response headers
            stepStopwatch.Restart();
            Response.Headers.ContentType = mediaType;
            if (response.Content.Headers.ContentLength.HasValue)
            {
                Response.Headers.ContentLength = response.Content.Headers.ContentLength.Value;
                _logger.LogInformation("📏 Content length: {ContentLength} bytes", response.Content.Headers.ContentLength.Value);
            }
            else
            {
                _logger.LogInformation("📏 Content length: Unknown (chunked transfer)");
            }
            Response.Headers.AcceptRanges = "bytes";
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 6 - Set response headers in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

            // Step 7: Get stream from YouTube response
            stepStopwatch.Restart();
            var stream = await response.Content.ReadAsStreamAsync();
            stepStopwatch.Stop();
            _logger.LogInformation("⏱️ Step 7 - Got YouTube stream in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);
            
            totalStopwatch.Stop();
            _logger.LogInformation("🎵 ✅ Started streaming '{Title}' by '{Artist}' - Total setup time: {TotalMs}ms", 
                musicResponse.TrackMetadata.Title, 
                musicResponse.TrackMetadata.Artist, 
                totalStopwatch.ElapsedMilliseconds);

            return new FileStreamResult(stream, mediaType)
            {
                EnableRangeProcessing = true,
                FileDownloadName = $"{musicResponse.TrackMetadata.Title}.{mediaType.Split('/')[1]}"
            };
        }
        catch (Exception ex)
        {
            totalStopwatch.Stop();
            _logger.LogError(ex, "❌ Error streaming audio after {ElapsedMs}ms: {ErrorMessage}", 
                totalStopwatch.ElapsedMilliseconds, ex.Message);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("search")]
    public async Task<ActionResult<IEnumerable<TrackMetadata>>> Search(
        [FromQuery] string query,
        [FromQuery] MusicSource? source = null,
        [FromQuery] int maxResults = 10)
    {
        var stopwatch = Stopwatch.StartNew();
        _logger.LogInformation("🔍 Starting search for query: '{Query}', source: {Source}, maxResults: {MaxResults}", 
            query, source?.ToString() ?? "Any", maxResults);

        try
        {
            if (string.IsNullOrEmpty(query))
            {
                return BadRequest("Query is required");
            }

            var results = await _musicService.SearchAsync(query, source, maxResults);
            stopwatch.Stop();
            
            var resultCount = results?.Count() ?? 0;
            _logger.LogInformation("🔍 ✅ Search completed: Found {ResultCount} results in {ElapsedMs}ms", 
                resultCount, stopwatch.ElapsedMilliseconds);
            
            return Ok(results);
        }
        catch (Exception ex)
        {
            stopwatch.Stop();
            _logger.LogError(ex, "❌ Search failed after {ElapsedMs}ms: {ErrorMessage}", 
                stopwatch.ElapsedMilliseconds, ex.Message);
            throw;
        }
    }
}