using Chrisalaxelrto.Bot.Services;
using Chrisalaxelrto.TrackStreamer.Providers;
using Chrisalaxelrto.TrackStreamer.Services;
using NetCord.Services.Commands;

namespace Chrisalaxelrto.Bot.Commands;

class MusicCommand : CommandModule<CommandContext>
{
    private readonly VoiceChannelService voiceChannelService;
    private readonly TrackMetadataService musicStreamerService;
    private readonly TrackStreamProvider trackStreamProvider;

    public MusicCommand(VoiceChannelService voiceChannelService, TrackMetadataService musicStreamerService, TrackStreamProvider trackStreamProvider)
    {
        this.voiceChannelService = voiceChannelService;
        this.musicStreamerService = musicStreamerService;
        this.trackStreamProvider = trackStreamProvider;
    }

    [Command("play", "p")]
    public async Task PlayMusic([CommandParameter(Remainder = true)] string searchQuery)
    {
        try
        {
            var sourceMetadata = await this.musicStreamerService.GetSourceMetadata(searchQuery);
            if (sourceMetadata == null)
            {
                await ReplyAsync("Failed to get track metadata.");
                return;
            }

            var musicStream = await this.trackStreamProvider.GetStream(sourceMetadata);
            if (musicStream == null)
            {
                await ReplyAsync("Failed to get music stream.");
                return;
            }
            
            await voiceChannelService.JoinVoiceChannel(Context);
            if (sourceMetadata != null)
            {
                await ReplyAsync($"🎵 Now playing: **{sourceMetadata.TrackMetadata.Title}** by **{sourceMetadata.TrackMetadata.Artist}**\n" +
                            $"⏱️ Duration: {sourceMetadata.TrackMetadata.Duration:mm\\:ss}\n" +
                            $"📺 Source: {sourceMetadata.TrackMetadata.Source}");
            }
            await voiceChannelService.PlayStream(Context, musicStream);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"Error: {ex.Message}");
        }
    }
}