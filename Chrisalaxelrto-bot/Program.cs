using NetCord;
using NetCord.Hosting.Gateway;
using NetCord.Hosting.Services;
using NetCord.Hosting.Services.ApplicationCommands;
using NetCord.Rest;
using NetCord.Gateway;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Azure.Identity;
using NetCord.Hosting.Services.Commands;


var builder = Host.CreateApplicationBuilder(args);

var env = Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT");
var isLocal = string.IsNullOrEmpty(env);
var configFile = isLocal ? "appsettings.json" : $"appsettings.{env}.json";
var configBasePath = isLocal ? Directory.GetCurrentDirectory() : "/var/www/";

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
builder.Configuration.AddAzureKeyVault(new Uri(keyVaultUri), new DefaultAzureCredential());

var token = isLocal ? builder.Configuration["discord-bot-token-dev"] : builder.Configuration["discord-bot-token"];
builder.Services
    .AddDiscordGateway(config =>
    {
        config.Token = token;
    })
    .AddCommands()
    .AddApplicationCommands();

builder.Services.AddSingleton<VoiceChannelService>();

var host = builder.Build();

host.AddModules(typeof(Program).Assembly);
host.AddApplicationCommandModule<MusicCommand>();
host.UseGatewayEventHandlers();

await host.RunAsync();