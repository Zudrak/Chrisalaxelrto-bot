using Chrisalaxelrto.Core.Providers.MusicStreamer;
using Chrisalaxelrto.Core.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Add HttpClient
builder.Services.AddHttpClient();

// Add Memory Cache
builder.Services.AddMemoryCache();

// Add Health Checks
builder.Services.AddHealthChecks();

// Register music source providers
builder.Services.AddScoped<IMusicSourceProvider, YouTubeMusicProvider>();

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
if (app.Environment.IsDevelopment())
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