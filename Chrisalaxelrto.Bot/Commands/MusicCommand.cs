using Chrisalaxelrto.Bot.Services;
using NetCord.Services.Commands;

namespace Chrisalaxelrto.Bot.Commands;
class MusicCommand : CommandModule<CommandContext>
{
    private VoiceChannelService voiceChannelService;

    public MusicCommand(VoiceChannelService voiceChannelService)
    {
        this.voiceChannelService = voiceChannelService;
    }

    [Command("play", "p")]
    public async Task PlayMusic(string searchQuery)
    {
        try
        {
            await voiceChannelService.JoinVoiceChannel(Context);
            await ReplyAsync($"Searching for: {searchQuery}");
        }
        catch (Exception ex)
        {
            await ReplyAsync($"Error: {ex.Message}");
        }
    }

    [Command("stop")]
    public async Task StopMusic()
    {
        try
        {
            await voiceChannelService.LeaveVoiceChannel(Context);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"Error: {ex.Message}");
        }
    }
}