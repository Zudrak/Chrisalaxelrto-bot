using Chrisalaxelrto.Bot.Commands;
using Chrisalaxelrto.Bot.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using NetCord.Hosting.Services.Commands;
namespace Chrisalaxelrto.Bot.Extensions;

static class PorebazuExtensions
{
    public static IServiceCollection AddPorebazuEventHandlers(this IServiceCollection services)
    {
        return services;
    }
    public static IServiceCollection AddPorebazuServices(this IServiceCollection services)
    {
        services.AddSingleton<VoiceChannelService>();
        services.AddHttpClient<MusicStreamerClient>(client =>
        {
            client.Timeout = TimeSpan.FromSeconds(200);
        });

        return services;
    }
    public static IHost AddPorebazuCommands(this IHost host)
    {
        host.AddCommandModule<MusicCommand>();
        return host;
    }

}