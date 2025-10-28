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
            var sourceMetadata = await trackPlayerService.EnqueueTrack(Context, searchQuery);
            if (sourceMetadata == null)
            {
                await ReplyAsync($"Track not found.");
                return;
            }
            
            await ReplyAsync($"Playing {sourceMetadata?.TrackMetadata.Title} - {sourceMetadata?.TrackMetadata.Artist}");
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }

    [Command("pause")]
    public async Task PauseMusic()
    {
        try
        {
            if (Context.Channel == null || Context.Guild == null)
            {
                throw new InvalidOperationException("This command can only be used in a guild channel.");
            }

            trackPlayerService.SetPaused(Context, true);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }

    [Command("resume")]
    public async Task ResumeMusic()
    {
        try
        {
            if (Context.Channel == null || Context.Guild == null)
            {
                throw new InvalidOperationException("This command can only be used in a guild channel.");
            }

            trackPlayerService.SetPaused(Context, false);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }
    
    [Command("stop")]
    public async Task StopMusic()
    {
        try
        {
            if (Context.Channel == null || Context.Guild == null)
            {
                throw new InvalidOperationException("This command can only be used in a guild channel.");
            }

            trackPlayerService.StopPlayback(Context);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"{ex.Message}");
        }
    }
}