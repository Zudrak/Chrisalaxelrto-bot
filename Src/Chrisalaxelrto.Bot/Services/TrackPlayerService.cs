using Chrisalaxelrto.Bot.Services;
using Chrisalaxelrto.TrackStreamer.Models;
using Chrisalaxelrto.TrackStreamer.Providers;
using Chrisalaxelrto.TrackStreamer.Services;
using NetCord.Services.Commands;

class TrackPlayerService
{
    private readonly TrackMetadataService trackMetadataService;
    private readonly TrackStreamProvider trackStreamProvider;
    private readonly VoiceChannelService voiceChannelService;
    private IDictionary<ulong, List<SourceMetadata>> trackQueues = new Dictionary<ulong, List<SourceMetadata>>();
    private Action<CommandContext> nextTrackAvailableCallback;
    private Task? activePlaybackTask;

    public TrackPlayerService(VoiceChannelService voiceChannelService, TrackMetadataService trackMetadataService, TrackStreamProvider trackStreamProvider)
    {
        this.trackMetadataService = trackMetadataService;
        this.trackStreamProvider = trackStreamProvider;
        this.voiceChannelService = voiceChannelService;

        this.nextTrackAvailableCallback = (CommandContext context) =>
        {
            PlayTrack(context, GetNextTrack(context));
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

        var musicStream = trackStreamProvider.GetStream(track).Result;

        if (musicStream != null)
        {
            activePlaybackTask = voiceChannelService.PlayStream(context, musicStream);
            await activePlaybackTask.ContinueWith(_ =>
            {
                trackQueues[context.Guild.Id].RemoveAt(0);
                nextTrackAvailableCallback.Invoke(context);
            });
        }
    }

    public IEnumerable<SourceMetadata> GetQueue(CommandContext context)
    {
        if (trackQueues.TryGetValue(context.Guild.Id, out var queue))
        {
            return queue;
        }
        return Enumerable.Empty<SourceMetadata>();
    }

    public void ClearQueue(CommandContext context)
    {
        if (trackQueues.ContainsKey(context.Guild.Id))
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
        if (trackQueues.TryGetValue(context.Guild.Id, out var queue) && queue.Count > 0)
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