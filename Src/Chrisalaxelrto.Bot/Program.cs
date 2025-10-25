using NetCord.Hosting.Gateway;
using NetCord.Hosting.Services.ApplicationCommands;
using NetCord.Gateway;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using NetCord.Hosting.Services.Commands;
using Chrisalaxelrto.Bot.Extensions;
using Microsoft.Extensions.DependencyInjection;


var builder = Host.CreateApplicationBuilder(args);

var env = Environment.GetEnvironmentVariable("CHRISALAXELRTO_ENV");
var isLocal = string.IsNullOrEmpty(env);
var configFile = isLocal ? "appsettings.json" : $"appsettings.{env}.json";
var configBasePath = isLocal ? Directory.GetCurrentDirectory() : Directory.GetCurrentDirectory();

// Explicitly set up configuration
builder.Configuration.SetBasePath(configBasePath)
    .AddJsonFile("base.appsettings.json", optional: false, reloadOnChange: true)
    .AddJsonFile(configFile, optional: false, reloadOnChange: true)
    .AddEnvironmentVariables()
    .Build();

var token = builder.Configuration["BotToken"];

if (string.IsNullOrEmpty(token))
{
    throw new InvalidOperationException("Bot token is not configured. Please set the \"BotToken\" variable in appsettings.json or environment.");
}

builder.Services
    .AddDiscordGateway(config =>
    {
        config.Token = token;
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