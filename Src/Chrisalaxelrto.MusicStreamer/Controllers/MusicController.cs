using Chrisalaxelrto.Core.Models.MusicStreamer;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Chrisalaxelrto.Core.Services;
using Microsoft.AspNetCore.Mvc;

namespace Chrisalaxelrto.MusicStreamer.Controllers;

[ApiController]
[Route("music")]
public class MusicController : ControllerBase
{
    private readonly MusicStreamingService _musicService;
    private readonly ILogger<MusicController> _logger;

    public MusicController(MusicStreamingService musicService, ILogger<MusicController> logger)
    {
        _musicService = musicService;
        _logger = logger;
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

            // Serialize metadata to JSON
            var metadataJson = System.Text.Json.JsonSerializer.Serialize(musicResponse.TrackMetadata);

            // Add metadata as a custom header
            Response.Headers.Append("X-Track-Metadata", Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes(metadataJson)));

            return new FileStreamResult(musicResponse.Stream, mediaType)
            {
                EnableRangeProcessing = true,
                FileDownloadName = musicResponse.TrackMetadata.Title + "." + mediaType.Split('/')[1]
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