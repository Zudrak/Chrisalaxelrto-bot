using Microsoft.Extensions.Hosting;
using NetCord.Hosting.Gateway;
using Microsoft.Extensions.Configuration;

var builder = Host.CreateApplicationBuilder(args);

var env = Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT");
var isLocal = string.IsNullOrEmpty(env);
var configFile = isLocal ? "appsettings.json" : $"appsettings.{env}.json";
var configBasePath = isLocal ? Directory.GetCurrentDirectory() : "/var/www/";

// Explicitly set up configuration
builder.Configuration.SetBasePath(configBasePath)
    .AddJsonFile("base.appsettings.json", optional: false, reloadOnChange: true)
    .AddJsonFile(configFile, optional: false, reloadOnChange: true)
    .AddEnvironmentVariables();

builder.Services.AddDiscordGateway();

var host = builder.Build();
await host.RunAsync();
