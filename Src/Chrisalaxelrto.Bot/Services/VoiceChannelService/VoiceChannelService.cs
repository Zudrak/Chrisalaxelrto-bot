using FFMpegCore;
using FFMpegCore.Pipes;
using NetCord.Gateway;
using NetCord.Gateway.Voice;
using NetCord.Services.Commands;

namespace Chrisalaxelrto.Bot.Services;

class VoiceChannelService
{
    private IDictionary<ulong, VoiceClient> voiceClients = new Dictionary<ulong, VoiceClient>();
    private IDictionary<ulong, PausableStream> pausableStreams = new Dictionary<ulong, PausableStream>();

    public bool IsInVoiceChannel(CommandContext context)
    {
        var guild = context.Guild;
        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }
        return voiceClients.ContainsKey(guild.Id);
    }

    public void SetPaused(CommandContext context, bool isPaused)
    {
        var guild = context.Guild;
        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }

        if (pausableStreams.TryGetValue(guild.Id, out var pausableStream))
        {
            pausableStream.IsPaused = isPaused;
        }
    }

    public async Task JoinVoiceChannel(CommandContext context, VoiceClientConfiguration? config = null)
    {
        var guild = context.Guild;

        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }

        if (!guild.VoiceStates.TryGetValue(context.User.Id, out var voiceState) || voiceState.ChannelId == null)
        {
            throw new InvalidOperationException("You must be in a voice channel to use this command.");
        }

        VoiceClient voiceClient;
        if (voiceClients.ContainsKey(guild.Id))
        {
            voiceClient = voiceClients[guild.Id];
        }
        else
        {
            voiceClient = await context.Client.JoinVoiceChannelAsync(guild.Id, voiceState.ChannelId.Value, config);
            if (voiceClient == null)
            {
                throw new InvalidOperationException("Failed to join voice channel.");
            }
            voiceClients.Add(guild.Id, voiceClient);
        }

        await voiceClient.StartAsync();
        await voiceClient.EnterSpeakingStateAsync(new SpeakingProperties(SpeakingFlags.Microphone));
    }

    public async Task LeaveVoiceChannel(CommandContext context)
    {
        var guild = context.Guild;
        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }
        if (voiceClients.TryGetValue(guild.Id, out var voiceClient))
        {
            await voiceClient.CloseAsync();
            await context.Client.UpdateVoiceStateAsync(new VoiceStateProperties(guild.Id, null));
            voiceClients.Remove(guild.Id);
        }

        // Clean up pausable stream
        if (pausableStreams.TryGetValue(guild.Id, out var pausableStream))
        {
            await pausableStream.DisposeAsync();
            pausableStreams.Remove(guild.Id);
        }
    }

    public async Task PlayStream(CommandContext context, Stream sourceStream, CancellationToken cancellationToken = default)
    {
        var guild = context.Guild;
        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }

        if (!voiceClients.TryGetValue(guild.Id, out var voiceClient))
        {
            await JoinVoiceChannel(context);
            voiceClient = voiceClients[guild.Id];
        }

        var outputStream = voiceClient.CreateOutputStream();

        // Wrap the OpusEncodeStream in a PausableStream
        OpusEncodeStream opusStream = new(outputStream, PcmFormat.Short, VoiceChannels.Stereo, OpusApplication.Audio);
        PausableStream pausableStream = new(opusStream);
        
        // Store the pausable stream so we can control it later
        if (pausableStreams.ContainsKey(guild.Id))
        {
            await pausableStreams[guild.Id].DisposeAsync();
        }
        pausableStreams[guild.Id] = pausableStream;

        try
        {
            // Use FFMpegCore to process the audio stream
            await FFMpegArguments
                .FromPipeInput(new StreamPipeSource(sourceStream))
                .OutputToPipe(new StreamPipeSink(pausableStream), options => options
                    .WithAudioCodec("pcm_s16le")
                    .WithAudioSamplingRate(48000)
                    .ForceFormat("s16le")
                    .WithCustomArgument("-ac 2")
                    .WithCustomArgument("-loglevel -8"))
                .CancellableThrough(cancellationToken)
                .ProcessAsynchronously();

            // Flush the pausable stream to ensure all audio data is sent
            await pausableStream.FlushAsync(cancellationToken);
        }
        finally
        {
            // Dispose the pausable stream
            await pausableStream.DisposeAsync();
            pausableStreams.Remove(guild.Id);
        }
    }

}