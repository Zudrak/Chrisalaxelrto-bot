using Microsoft.AspNetCore.Mvc;

namespace Chrisalaxelrto.MusicStreamer.Controllers;

[ApiController]
[Route("[controller]")]
public class HealthController : ControllerBase
{
    private readonly ILogger<HealthController> _logger;

    public HealthController(ILogger<HealthController> logger)
    {
        _logger = logger;
    }

    [HttpGet]
    public IActionResult Get()
    {
        return Ok(new { status = "Healthy", timestamp = DateTime.UtcNow });
    }

    [HttpGet("ready")]
    public IActionResult Ready()
    {
        // Add any readiness checks here (database connectivity, etc.)
        return Ok(new { status = "Ready", timestamp = DateTime.UtcNow });
    }
}