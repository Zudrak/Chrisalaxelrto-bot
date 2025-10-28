using NetCord.Hosting.Gateway;
using NetCord.Hosting.Services.ApplicationCommands;
using NetCord.Gateway;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using NetCord.Hosting.Services.Commands;
using Chrisalaxelrto.Bot.Extensions;
using Microsoft.Extensions.DependencyInjection;
using Chrisalaxelrto.Bot.Services;


var builder = Host.CreateApplicationBuilder(args);

var env = Environment.GetEnvironmentVariable("CHRISALAXELRTO_ENV");
var isLocal = string.IsNullOrEmpty(env);
var configFile = isLocal ? "appsettings.json" : $"appsettings.{env}.json";
var configBasePath = isLocal ? Directory.GetCurrentDirectory() : Directory.GetCurrentDirectory();

var secretStorage = new SecretStorageService();

if (!secretStorage.AreAllSecretsLoaded())
{
    secretStorage.RequestSecretsFromConsole();
}

// Explicitly set up configuration
builder.Configuration.SetBasePath(configBasePath)
    .AddJsonFile("base.appsettings.json", optional: false, reloadOnChange: true)
    .AddJsonFile(configFile, optional: false, reloadOnChange: true)
    .AddEnvironmentVariables()
    .AddSecretStorage(secretStorage)
    .AddCommandLine(args)
    .Build();

builder.Services
    .AddDiscordGateway(config =>
    {
        config.Token = builder.Configuration["BotToken"];
        config.Intents = GatewayIntents.Guilds | GatewayIntents.GuildMessages | GatewayIntents.MessageContent | GatewayIntents.GuildVoiceStates;
    })
    .AddCommands()
    .AddApplicationCommands();

builder.Services.AddMemoryCache();
builder.Services.AddPorebazuEventHandlers();
builder.Services.AddPorebazuServices();

var host = builder.Build();

host.AddPorebazuCommands();

await host.RunAsync();