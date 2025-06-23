using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Services;
using Microsoft.AspNetCore.Mvc;

namespace Chrisalaxelrto.MusicStreamer.Controllers;

[ApiController]
[Route("api/music")]
public class MusicController : ControllerBase
{
    private readonly MusicStreamingService _musicService;
    private readonly ILogger<MusicController> _logger;

    public MusicController(MusicStreamingService musicService, ILogger<MusicController> logger)
    {
        _musicService = musicService;
        _logger = logger;
    }

    [HttpPost("stream")]
    public async Task<ActionResult<StreamResponse>> GetStream([FromBody] StreamRequest request)
    {
        if (string.IsNullOrEmpty(request.Url))
        {
            return BadRequest("URL is required");
        }

        var result = await _musicService.GetStreamAsync(request);
        
        if (!result.Success)
        {
            return BadRequest(result.Message);
        }

        return Ok(result);
    }

    [HttpGet("stream/audio")]
    public async Task<IActionResult> StreamAudio([FromQuery] string url, [FromQuery] AudioQuality quality = AudioQuality.Medium)
    {
        try
        {
            var streamRequest = new StreamRequest { Url = url, Quality = quality };
            var streamInfo = await _musicService.GetStreamAsync(streamRequest);
            
            if (!streamInfo.Success || streamInfo.Stream == null)
            {
                return BadRequest("Failed to get stream information");
            }

            var audioStream = await _musicService.GetAudioDataAsync(url);
            
            return new FileStreamResult(audioStream, "audio/mpeg")
            {
                FileDownloadName = $"{streamInfo.Stream.Title}.mp3",
                EnableRangeProcessing = true
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error streaming audio for URL: {Url}", url);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("search")]
    public async Task<ActionResult<IEnumerable<AudioStream>>> Search(
        [FromQuery] string query,
        [FromQuery] string? source = null,
        [FromQuery] int maxResults = 10)
    {
        if (string.IsNullOrEmpty(query))
        {
            return BadRequest("Query is required");
        }

        var results = await _musicService.SearchAsync(query, source, maxResults);
        return Ok(results);
    }

    [HttpGet("sources")]
    public ActionResult<IEnumerable<string>> GetAvailableSources()
    {
        var sources = _musicService.GetAvailableSources();
        return Ok(sources);
    }

    [HttpGet("info")]
    public async Task<ActionResult<AudioStream>> GetTrackInfo([FromQuery] string url)
    {
        if (string.IsNullOrEmpty(url))
        {
            return BadRequest("URL is required");
        }

        var streamRequest = new StreamRequest { Url = url };
        var result = await _musicService.GetStreamAsync(streamRequest);
        
        if (!result.Success || result.Stream == null)
        {
            return BadRequest(result.Message);
        }

        return Ok(result.Stream);
    }
}