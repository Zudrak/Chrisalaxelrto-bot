using System.Net;
using System.Text;
using System.Text.Json;

public static class CookieParser
{
    public static IReadOnlyList<Cookie> ParseCookies(string cookiesBase64)
    {
        try
        {
            // Decode the base64 string
            var cookiesJson = Encoding.UTF8.GetString(Convert.FromBase64String(cookiesBase64));

            // Deserialize the JSON to a list of cookies
            var cookies = JsonSerializer.Deserialize<List<Cookie>>(cookiesJson, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });

            if (cookies == null)
            {
                throw new Exception("Deserialized cookies are null.");
            }
            
            return cookies.AsReadOnly();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Failed to parse base64 JSON cookies. Error: {ex.Message}");
            throw;
        }
    }
}