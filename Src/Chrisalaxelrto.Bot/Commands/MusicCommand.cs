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
            if (Context.Channel == null || Context.Guild == null)
            {
                throw new InvalidOperationException("This command can only be used in a guild channel.");
            }
            
            await Context.Channel.TriggerTypingStateAsync();
            await trackPlayerService.EnqueueTrack(Context, searchQuery);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }
}