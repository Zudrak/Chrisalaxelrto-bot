using Chrisalaxelrto.Bot.Services;
using Chrisalaxelrto.TrackStreamer.Models;
using Chrisalaxelrto.TrackStreamer.Providers;
using Chrisalaxelrto.TrackStreamer.Services;
using NetCord.Services.Commands;

class TrackPlayerService
{
    private readonly TrackMetadataService trackMetadataService;
    private readonly VoiceChannelService voiceChannelService;
    private IDictionary<ulong, List<SourceMetadata>> trackQueues = new Dictionary<ulong, List<SourceMetadata>>();
    private Action<CommandContext> nextTrackAvailableCallback;
    private Task? activePlaybackTask;

    public TrackPlayerService(VoiceChannelService voiceChannelService, TrackMetadataService trackMetadataService)
    {
        this.trackMetadataService = trackMetadataService;
        this.voiceChannelService = voiceChannelService;

        this.nextTrackAvailableCallback = (CommandContext context) =>
        {
            _ = PlayTrack(context, GetNextTrack(context));
        };
    }

    public async Task EnqueueTrack(CommandContext context, string searchQuery)
    {
        ValidateContext(context);

        var sourceMetadata = await trackMetadataService.GetSourceMetadata(searchQuery);
        if (sourceMetadata == null)
        {
            throw new InvalidOperationException("Track not found.");
        }

        if (context.Guild == null || context.Channel == null)
        {
            throw new InvalidOperationException("Guild or Channel not found.");
        }

        if (!trackQueues.ContainsKey(context.Guild.Id))
        {
            trackQueues[context.Guild.Id] = new List<SourceMetadata>();
        }

        await context.Channel.SendMessageAsync($"Enqueued: {sourceMetadata.TrackMetadata.Title}");
        trackQueues[context.Guild.Id].Add(sourceMetadata);

        if (activePlaybackTask == null || activePlaybackTask.IsCompleted)
        {
            await PlayTrack(context, GetNextTrack(context));
        }
    }

    public async Task PlayTrack(CommandContext context, SourceMetadata? track)
    {
        if (track == null)
        {
            throw new InvalidOperationException("No track to play.");
        }

        var musicStream = await trackMetadataService.GetStream(track);

        if (musicStream != null)
        {
            // Store the task but don't await it here - let it run in background
            activePlaybackTask = PlayTrackInternal(context, musicStream, track);
        }
    }

    private async Task PlayTrackInternal(CommandContext context, Stream musicStream, SourceMetadata track)
    {
        try
        {
            await voiceChannelService.PlayStream(context, musicStream);
        }
        catch (Exception ex)
        {
            // Log or handle playback errors
            if (context.Channel != null)
            {
                await context.Channel.SendMessageAsync($"Error playing track: {ex.Message}");
            }
        }
        finally
        {
            if (context.Guild != null && trackQueues.ContainsKey(context.Guild.Id) && trackQueues[context.Guild.Id].Count > 0)
            {
                trackQueues[context.Guild.Id].RemoveAt(0);
            }
            
            // Play next track if available
            var nextTrack = GetNextTrack(context);
            if (nextTrack != null)
            {
                await PlayTrack(context, nextTrack);
            }
        }
    }

    public IEnumerable<SourceMetadata> GetQueue(CommandContext context)
    {
        if (context.Guild != null && trackQueues.TryGetValue(context.Guild.Id, out var queue))
        {
            return queue;
        }
        return Enumerable.Empty<SourceMetadata>();
    }

    public void ClearQueue(CommandContext context)
    {
        if (context.Guild != null && trackQueues.ContainsKey(context.Guild.Id))
        {
            trackQueues[context.Guild.Id].Clear();
        }
    }
    public void RegisterNextTrackAvailableCallback(Action<CommandContext> callback)
    {
        nextTrackAvailableCallback += callback;
    }

    private SourceMetadata? GetNextTrack(CommandContext context)
    {
        if (context.Guild != null && trackQueues.TryGetValue(context.Guild.Id, out var queue) && queue.Count > 0)
        {
            var track = queue[0];
            return track;
        }
        return null;
    }   

    private void ValidateContext(CommandContext context)
    {
        if (context.Guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }

        if (context.Channel == null)
        {
            throw new InvalidOperationException("Channel not found. Ensure the command is used in a guild context.");
        }

    }
}