using NetCord.Gateway;
using NetCord.Gateway.Voice;
using NetCord.Services.Commands;
using System.Diagnostics;
using System.Threading.Tasks;

namespace Chrisalaxelrto.Bot.Services;

class VoiceChannelService
{
    private IDictionary<ulong, VoiceClient> voiceClients = new Dictionary<ulong, VoiceClient>();
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
    }
    public async Task PlayStream(CommandContext context, Stream sourceStream) 
    {
        var guild = context.Guild;
        if (guild == null)
        {
            throw new InvalidOperationException("Guild not found. Ensure the command is used in a guild context.");
        }
        
        if (!voiceClients.TryGetValue(guild.Id, out var voiceClient))
        {
            throw new InvalidOperationException("Could't find a voice client for this session.");
        }

        var outputStream = voiceClient.CreateOutputStream();
        OpusEncodeStream stream = new(outputStream, PcmFormat.Short, VoiceChannels.Stereo, OpusApplication.Audio);

        ProcessStartInfo startInfo = new("ffmpeg")
        {
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
        };
        var arguments = startInfo.ArgumentList;

        // Specify the input as coming from stdin
        arguments.Add("-i");
        arguments.Add("pipe:0");

        // Set the logging level to quiet mode
        arguments.Add("-loglevel");
        arguments.Add("-8");

        // Set the number of audio channels to 2 (stereo)
        arguments.Add("-ac");
        arguments.Add("2");

        // Set the output format to 16-bit signed little-endian
        arguments.Add("-f");
        arguments.Add("s16le");

        // Set the audio sampling rate to 48 kHz
        arguments.Add("-ar");
        arguments.Add("48000");

        // Direct the output to stdout
        arguments.Add("pipe:1");

        var ffmpeg = Process.Start(startInfo)!;

        // Copy the source stream to ffmpeg's stdin and ffmpeg's stdout to the voice stream
        var inputTask = sourceStream.CopyToAsync(ffmpeg.StandardInput.BaseStream);
        var outputTask = ffmpeg.StandardOutput.BaseStream.CopyToAsync(stream);

        // Close stdin after copying is complete to signal end of input
        inputTask.ContinueWith(_ => ffmpeg.StandardInput.Close());

        await Task.WhenAll(inputTask, outputTask);
        await outputStream.FlushAsync();
    }

}