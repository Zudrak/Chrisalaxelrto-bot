using NetCord.Gateway;
using NetCord.Gateway.Voice;
using NetCord.Services.Commands;

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

}