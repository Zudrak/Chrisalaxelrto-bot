using NetCord.Hosting.Gateway;
using NetCord.Hosting.Services.ApplicationCommands;
using NetCord.Gateway;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using Azure.Identity;
using NetCord.Hosting.Services.Commands;
using Chrisalaxelrto.Bot.Extensions;


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
        config.Intents = GatewayIntents.Guilds | GatewayIntents.GuildMessages | GatewayIntents.MessageContent | GatewayIntents.GuildVoiceStates;
    })
    .AddCommands()
    .AddApplicationCommands();

builder.Services.AddPorebazuEventHandlers();
builder.Services.AddPorebazuServices();

var host = builder.Build();

host.AddPorebazuCommands();
host.UseGatewayEventHandlers();

await host.RunAsync();