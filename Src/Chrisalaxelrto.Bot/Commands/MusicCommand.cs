using Chrisalaxelrto.Bot.Services;
using NetCord.Services.Commands;

namespace Chrisalaxelrto.Bot.Commands;

class MusicCommand : CommandModule<CommandContext>
{
    private readonly VoiceChannelService _voiceChannelService;
    private readonly MusicStreamerClient _musicStreamerService;

    public MusicCommand(VoiceChannelService voiceChannelService, MusicStreamerClient musicStreamerService)
    {
        _voiceChannelService = voiceChannelService;
        _musicStreamerService = musicStreamerService;
    }

    [Command("play", "p")]
    public async Task PlayMusic([CommandParameter(Remainder = true)] string searchQuery)
    {
        try
        {
            await ReplyAsync($"🔍 Searching for: {searchQuery}");

            var (stream, trackMetadata) = await _musicStreamerService.GetStreamAsync(searchQuery);

            if (stream == null)
            {
                await ReplyAsync("❌ No results found for your search.");
                return;
            }

            // Join voice channel
            await _voiceChannelService.JoinVoiceChannel(Context);
            if (trackMetadata != null)
            {
                await ReplyAsync($"🎵 Now playing: **{trackMetadata.Title}** by **{trackMetadata.Artist}**\n" +
                            $"⏱️ Duration: {trackMetadata.Duration:mm\\:ss}\n" +
                            $"📺 Source: {trackMetadata.Source}");
            }
            await _voiceChannelService.PlayStream(Context, stream);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"❌ Error: {ex.Message}");
        }
    }

    [Command("search")]
    public async Task SearchMusic([CommandParameter(Remainder = true)] string query)
    {
        try
        {
            await ReplyAsync($"🔍 Searching for: {query}");

            var searchResults = await _musicStreamerService.SearchAsync(query, maxResults: 10);
            
            if (!searchResults.Any())
            {
                await ReplyAsync("❌ No results found.");
                return;
            }

            var response = "🎵 **Search Results:**\n";
            var index = 1;
            
            foreach (var result in searchResults.Take(10))
            {
                response += $"{index}. **{result.Title}** by **{result.Artist}**\n" +
                          $"   ⏱️ {result.Duration:mm\\:ss} | 📺 {result.Source}\n";
                index++;
            }

            await ReplyAsync(response);
        }
        catch (Exception ex)
        {
            await ReplyAsync($"❌ Error searching: {ex.Message}");
        }
    }

    [Command("stop")]
    public async Task StopMusic()
    {
        try
        {
            await _voiceChannelService.LeaveVoiceChannel(Context);
            await ReplyAsync("⏹️ Stopped music and left voice channel.");
        }
        catch (Exception ex)
        {
            await ReplyAsync($"❌ Error: {ex.Message}");
        }
    }
}