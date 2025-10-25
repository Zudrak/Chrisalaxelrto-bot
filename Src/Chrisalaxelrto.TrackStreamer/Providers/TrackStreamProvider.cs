using Chrisalaxelrto.TrackStreamer.Models;

namespace Chrisalaxelrto.TrackStreamer.Providers;

public class TrackStreamProvider
{
    private readonly HttpClient httpClient;

    public TrackStreamProvider(HttpClient httpClient)
    {
            this.httpClient = httpClient;
    }

    public async Task<Stream?> GetStream(SourceMetadata sourceMetadata)
    {   
        var response = await httpClient.GetAsync(sourceMetadata.StreamUrl, HttpCompletionOption.ResponseHeadersRead);
        if (response.IsSuccessStatusCode)
        {
            return await response.Content.ReadAsStreamAsync();
        }
        return null;
    }
}