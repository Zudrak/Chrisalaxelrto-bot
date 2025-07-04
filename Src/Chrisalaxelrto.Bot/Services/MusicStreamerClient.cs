using Chrisalaxelrto.Core.Models.MusicStreamer;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using Microsoft.ApplicationInsights;
using System.Diagnostics;
using Chrisalaxelrto.Core.Providers.MusicStreamer;

namespace Chrisalaxelrto.Bot.Services;

public class MusicStreamerClient
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<MusicStreamerClient> _logger;
    private readonly TelemetryClient? _telemetryClient;
    private readonly string _baseUrl;

    public MusicStreamerClient(HttpClient httpClient, IConfiguration configuration, ILogger<MusicStreamerClient> logger, TelemetryClient? telemetryClient = null)
    {
        _httpClient = httpClient;
        _logger = logger;
        _telemetryClient = telemetryClient;
        _baseUrl = configuration["MusicStreamerBaseUrl"] ?? throw new InvalidOperationException("MusicStreamerBaseUrl configuration is required.");
        
        _httpClient.BaseAddress = new Uri(_baseUrl);
    }

    public async Task<IEnumerable<TrackMetadata>> SearchAsync(string query, MusicSource? source = null, int maxResults = 10)
    {
        var stopwatch = Stopwatch.StartNew();
        
        _logger.LogInformation("Starting music search for query: {Query}, source: {Source}, maxResults: {MaxResults}", 
            query, source?.ToString() ?? "Any", maxResults);

        try
        {
            var queryParams = $"?query={Uri.EscapeDataString(query)}&maxResults={maxResults}";
            if (source.HasValue)
            {
                queryParams += $"&source={source}";
            }

            var response = await _httpClient.GetAsync($"music/search{queryParams}");
            response.EnsureSuccessStatusCode();

            var content = await response.Content.ReadAsStringAsync();
            var results = JsonSerializer.Deserialize<IEnumerable<TrackMetadata>>(content, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });

            var resultCount = results?.Count() ?? 0;
            stopwatch.Stop();

            // ILogger for operational details
            _logger.LogInformation("Music search completed successfully. Found {ResultCount} results in {ElapsedMs}ms", 
                resultCount, stopwatch.ElapsedMilliseconds);

            // TelemetryClient for business metrics
            _telemetryClient?.TrackDependency("MusicStreamer", "Search", query, DateTime.UtcNow.Subtract(stopwatch.Elapsed), stopwatch.Elapsed, true);
            _telemetryClient?.TrackMetric("MusicSearch.ResultCount", resultCount);
            _telemetryClient?.TrackMetric("MusicSearch.Duration", stopwatch.ElapsedMilliseconds);

            return results ?? Enumerable.Empty<TrackMetadata>();
        }
        catch (Exception ex)
        {
            stopwatch.Stop();
            
            // ILogger for detailed error information (developers need this)
            _logger.LogError(ex, "Failed to search music. Query: {Query}, Source: {Source}, Error: {ErrorMessage}", 
                query, source?.ToString() ?? "Any", ex.Message);

            // TelemetryClient for monitoring/alerting (operations team needs this)
            _telemetryClient?.TrackDependency("MusicStreamer", "Search", query, DateTime.UtcNow.Subtract(stopwatch.Elapsed), stopwatch.Elapsed, false);
            _telemetryClient?.TrackException(ex, new Dictionary<string, string>
            {
                ["Operation"] = "SearchMusic",
                ["Query"] = query,
                ["Source"] = source?.ToString() ?? "Any"
            });

            throw;
        }
    }
    
public async Task<(Stream Stream, TrackMetadata? Metadata)> GetStreamAsync(string musicQuery, AudioQuality quality = AudioQuality.VeryHigh)
{
    var totalStopwatch = Stopwatch.StartNew();
    var stepStopwatch = Stopwatch.StartNew();

    _logger.LogInformation("🤖 Bot: Starting stream request for query: '{Query}' with quality: {Quality}", musicQuery, quality);

    try
    {
        // Step 1: Prepare request
        stepStopwatch.Restart();
        var queryParams = $"?musicQuery={Uri.EscapeDataString(musicQuery)}&quality={quality}";
        stepStopwatch.Stop();
        _logger.LogInformation("🤖 ⏱️ Step 1 - Prepared request params in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

        // Step 2: Send request to Music Streamer
        stepStopwatch.Restart();
        _logger.LogInformation("🤖 📡 Sending request to Music Streamer: {Url}", $"music/stream{queryParams}");
        var response = await _httpClient.GetAsync($"music/stream{queryParams}", HttpCompletionOption.ResponseHeadersRead);
        stepStopwatch.Stop();
        _logger.LogInformation("🤖 ⏱️ Step 2 - Got response from Music Streamer in {ElapsedMs}ms, Status: {StatusCode}", 
            stepStopwatch.ElapsedMilliseconds, response.StatusCode);

        if (!response.IsSuccessStatusCode)
        {
            var errorContent = await response.Content.ReadAsStringAsync();
            totalStopwatch.Stop();

            _logger.LogWarning("🤖 ❌ Music stream request failed. Query: {Query}, Status: {StatusCode}, Response: {Response}",
                musicQuery, response.StatusCode, errorContent);

            _telemetryClient?.TrackDependency("MusicStreamer", "Stream", musicQuery, DateTime.UtcNow.Subtract(totalStopwatch.Elapsed), totalStopwatch.Elapsed, false);

            throw new HttpRequestException($"Request failed with status {response.StatusCode}: {errorContent}");
        }

        // Step 3: Parse metadata from headers
        stepStopwatch.Restart();
        TrackMetadata? metadata = null;
        if (response.Headers.TryGetValues("X-Track-Metadata", out var metadataHeaders))
        {
            try
            {
                var metadataBase64 = metadataHeaders.First();
                var metadataJson = System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(metadataBase64));
                metadata = JsonSerializer.Deserialize<TrackMetadata>(metadataJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                _logger.LogDebug("🤖 Successfully parsed track metadata: {Title} by {Artist}", metadata?.Title, metadata?.Artist);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "🤖 Failed to parse track metadata from response headers");
            }
        }
        stepStopwatch.Stop();
        _logger.LogInformation("🤖 ⏱️ Step 3 - Parsed metadata in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

        // Step 4: Get stream
        stepStopwatch.Restart();
        var stream = await response.Content.ReadAsStreamAsync();
        stepStopwatch.Stop();
        _logger.LogInformation("🤖 ⏱️ Step 4 - Got stream in {ElapsedMs}ms", stepStopwatch.ElapsedMilliseconds);

        totalStopwatch.Stop();
        _logger.LogInformation("🤖 ✅ Successfully started streaming for '{Title}' by '{Artist}' - Total time: {TotalMs}ms",
            metadata?.Title ?? "Unknown", metadata?.Artist ?? "Unknown", totalStopwatch.ElapsedMilliseconds);

        _telemetryClient?.TrackDependency("MusicStreamer", "Stream", musicQuery, DateTime.UtcNow.Subtract(totalStopwatch.Elapsed), totalStopwatch.Elapsed, true);
        _telemetryClient?.TrackEvent("MusicStreamSuccess", new Dictionary<string, string>
        {
            ["Title"] = metadata?.Title ?? "Unknown",
            ["Artist"] = metadata?.Artist ?? "Unknown",
            ["Source"] = metadata?.Source.ToString() ?? "Unknown",
            ["Quality"] = quality.ToString()
        });
        _telemetryClient?.TrackMetric("MusicStream.Duration", totalStopwatch.ElapsedMilliseconds);

        return (stream, metadata);
    }
    catch (Exception ex)
    {
        totalStopwatch.Stop();

        _logger.LogError(ex, "🤖 ❌ Failed to get music stream after {ElapsedMs}ms. Query: {Query}, Quality: {Quality}", 
            totalStopwatch.ElapsedMilliseconds, musicQuery, quality);

        _telemetryClient?.TrackDependency("MusicStreamer", "Stream", musicQuery, DateTime.UtcNow.Subtract(totalStopwatch.Elapsed), totalStopwatch.Elapsed, false);
        _telemetryClient?.TrackException(ex, new Dictionary<string, string>
        {
            ["Operation"] = "GetStream",
            ["Query"] = musicQuery,
            ["Quality"] = quality.ToString()
        });

        throw;
    }
}
    public string GetStreamUrl(string musicQuery, AudioQuality quality = AudioQuality.VeryHigh)
    {
        var queryParams = $"?musicQuery={Uri.EscapeDataString(musicQuery)}&quality={quality}";
        return $"{_baseUrl}/music/stream{queryParams}";
    }
}