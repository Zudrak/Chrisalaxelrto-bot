// Simple HTTP service for health checks
using System.Net;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

public class HealthCheckHttpService : BackgroundService
{
    private readonly ILogger<HealthCheckHttpService> _logger;
    private readonly HealthCheckService _healthCheckService;
    private HttpListener? _listener;

    public HealthCheckHttpService(ILogger<HealthCheckHttpService> logger, HealthCheckService healthCheckService)
    {
        _logger = logger;
        _healthCheckService = healthCheckService;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _listener = new HttpListener();
            _listener.Prefixes.Add("http://+:8080/");
            _listener.Start();
            _logger.LogInformation("Health check HTTP listener started on port 8080");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var context = await _listener.GetContextAsync();
                    _ = Task.Run(async () => await HandleRequest(context), stoppingToken);
                }
                catch (HttpListenerException) when (stoppingToken.IsCancellationRequested)
                {
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error in health check listener");
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to start health check HTTP listener");
        }
    }

    private async Task HandleRequest(HttpListenerContext context)
    {
        try
        {
            var request = context.Request;
            var response = context.Response;

            if (request.Url?.AbsolutePath == "/health")
            {
                var healthReport = await _healthCheckService.CheckHealthAsync();
                var status = healthReport.Status == HealthStatus.Healthy ? 200 : 503;
                
                response.StatusCode = status;
                response.ContentType = "application/json";
                
                var result = new
                {
                    status = healthReport.Status.ToString(),
                    checks = healthReport.Entries.Select(entry => new
                    {
                        name = entry.Key,
                        status = entry.Value.Status.ToString(),
                        description = entry.Value.Description
                    })
                };

                var json = System.Text.Json.JsonSerializer.Serialize(result);
                var buffer = System.Text.Encoding.UTF8.GetBytes(json);
                await response.OutputStream.WriteAsync(buffer, 0, buffer.Length);
            }
            else if (request.Url?.AbsolutePath == "/health/live")
            {
                response.StatusCode = 200;
                response.ContentType = "text/plain";
                var buffer = System.Text.Encoding.UTF8.GetBytes("OK");
                await response.OutputStream.WriteAsync(buffer, 0, buffer.Length);
            }
            else
            {
                response.StatusCode = 404;
                var buffer = System.Text.Encoding.UTF8.GetBytes("Not Found");
                await response.OutputStream.WriteAsync(buffer, 0, buffer.Length);
            }

            response.Close();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling health check request");
            try
            {
                context.Response.StatusCode = 500;
                context.Response.Close();
            }
            catch { }
        }
    }

    public override void Dispose()
    {
        _listener?.Stop();
        _listener?.Close();
        base.Dispose();
    }
}