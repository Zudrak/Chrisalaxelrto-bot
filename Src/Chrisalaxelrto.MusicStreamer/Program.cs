using Azure.Identity;
using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Chrisalaxelrto.Core.Services;
using Chrisalaxelrto.MusicStreamer.Providers.Youtube;

var builder = WebApplication.CreateBuilder(args);

var env = Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT");
var isLocal = string.IsNullOrEmpty(env);
var configFile = isLocal ? "appsettings.json" : $"appsettings.{env}.json";
var configBasePath = isLocal ? Directory.GetCurrentDirectory() : Directory.GetCurrentDirectory();

// Explicitly set up configuration
builder.Configuration.SetBasePath(configBasePath)
    .AddJsonFile("base.appsettings.json", optional: false, reloadOnChange: true)
    .AddJsonFile(configFile, optional: false, reloadOnChange: true)
    .AddEnvironmentVariables()
    .Build();

var keyVaultUri = builder.Configuration["AzureKeyVaultUri"];
if (string.IsNullOrEmpty(keyVaultUri))
{
    throw new InvalidOperationException("AzureKeyVaultUri configuration is required.");
}
builder.Configuration.AddAzureKeyVault(
    new Uri(keyVaultUri),
    new DefaultAzureCredential(
        new DefaultAzureCredentialOptions
        {
            ManagedIdentityClientId = builder.Configuration["ManagedIdentityClientId"]
        }
    ));

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddApplicationInsightsTelemetry(options =>
{
    options.ConnectionString = builder.Configuration.GetConnectionString("ApplicationInsights");
});

// Add HttpClient
builder.Services.AddHttpClient();

// Add Memory Cache
builder.Services.AddMemoryCache();

// Add Health Checks
builder.Services.AddHealthChecks();

// Load cookies from configuration
var cookieString = builder.Configuration["youtube-cookies"];
if (string.IsNullOrEmpty(cookieString))
{
    throw new InvalidOperationException("YouTubeCookies configuration is required.");
}

// Register music source providers
builder.Services.AddTransient<IMusicSourceProvider, YouTubeMusicProvider>();

// Register music streaming service
builder.Services.AddScoped<MusicStreamingService>();

// Add CORS for web clients
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", builder =>
    {
        builder.AllowAnyOrigin()
               .AllowAnyMethod()
               .AllowAnyHeader();
    });
});

var app = builder.Build();

// Configure the HTTP request pipeline.
if (isLocal)
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Don't redirect to HTTPS in containers (handled by ingress)
// app.UseHttpsRedirection();

app.UseCors("AllowAll");
app.UseAuthorization();
app.MapControllers();
app.MapHealthChecks("/health");

app.Run();