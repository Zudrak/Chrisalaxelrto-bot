using NetCord.Rest;
using NetCord.Services.ApplicationCommands;
using NetCord.Services.Commands;

class MusicCommand : ApplicationCommandModule<ApplicationCommandContext>
{
    private VoiceChannelService _voiceChannelService;

    public MusicCommand(VoiceChannelService voiceChannelService)
    {
        _voiceChannelService = voiceChannelService;
    }

    [Command("p", "play")]
    public async Task PlayMusicAsync(string searchQuery)
    {
        try
        {
            await _voiceChannelService.JoinVoiceChannel(Context);
            await RespondAsync(InteractionCallback.Message($"Joined the voice channel successfully!"));
        }
        catch (Exception ex)
        {
            await RespondAsync(InteractionCallback.Message($"{ex.Message}"));
        }
    }
}