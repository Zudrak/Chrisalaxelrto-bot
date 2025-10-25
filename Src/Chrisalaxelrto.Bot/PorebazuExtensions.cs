using Chrisalaxelrto.Bot.Commands;
using Chrisalaxelrto.Bot.Services;
using Chrisalaxelrto.TrackStreamer.Providers;
using Chrisalaxelrto.TrackStreamer.Providers.Youtube;
using Chrisalaxelrto.TrackStreamer.Services;
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
        services.AddTransient<ITrackSourceProvider, YouTubeTrackProvider>();
        services.AddScoped<TrackMetadataService>();
        services.AddHttpClient<TrackStreamProvider>();
        services.AddSingleton<VoiceChannelService>();
        services.AddSingleton<TrackPlayerService>();
        return services;
    }
    public static IHost AddPorebazuCommands(this IHost host)
    {
        host.AddCommandModule<MusicCommand>();
        return host;
    }

}