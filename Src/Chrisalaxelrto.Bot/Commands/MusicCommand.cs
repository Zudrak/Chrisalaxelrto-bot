using NetCord.Services.Commands;

namespace Chrisalaxelrto.Bot.Commands;

class MusicCommand : CommandModule<CommandContext>
{
    private readonly TrackPlayerService trackPlayerService;

    public MusicCommand(TrackPlayerService trackPlayerService)
    {
        this.trackPlayerService = trackPlayerService;
    }

    [Command("play", "p")]
    public async Task PlayMusic([CommandParameter(Remainder = true)] string searchQuery)
    {
        try
        {
            await Context.Channel.TriggerTypingStateAsync();
            trackPlayerService.EnqueueTrack(Context, searchQuery);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }
}