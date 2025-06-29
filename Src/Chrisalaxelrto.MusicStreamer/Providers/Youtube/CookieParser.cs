using System.Net;

public static class CookieParser
{
    /// <summary>
    /// Parses a Netscape cookie file format string into a list of System.Net.Cookie objects
    /// </summary>
    /// <param name="cookieFileContent">The content of the Netscape cookie file</param>
    /// <returns>A list of cookies that can be used with YoutubeExplode</returns>
    public static IReadOnlyList<Cookie> ParseNetscapeCookies(string cookieFileContent)
    {
        var cookies = new List<Cookie>();
        var lines = cookieFileContent.Split('\n', StringSplitOptions.RemoveEmptyEntries);
        
        foreach (var line in lines)
        {
            // Skip comments and empty lines
            if (line.StartsWith("#") || string.IsNullOrWhiteSpace(line))
                continue;
                
            var parts = line.Split('\t');
            if (parts.Length < 7)
                continue;
                
            try
            {
                var domain = parts[0];
                var includeSubdomains = parts[1].Equals("TRUE", StringComparison.OrdinalIgnoreCase);
                var path = parts[2];
                var secure = parts[3].Equals("TRUE", StringComparison.OrdinalIgnoreCase);
                var expirationTimestamp = parts[4];
                var name = parts[5];
                var value = parts[6];
                
                var cookie = new Cookie(name, value, path, domain)
                {
                    Secure = secure
                };
                
                // Handle expiration
                if (long.TryParse(expirationTimestamp, out var timestamp) && timestamp > 0)
                {
                    var expirationDate = DateTimeOffset.FromUnixTimeSeconds(timestamp);
                    cookie.Expires = expirationDate.DateTime;
                }
                
                cookies.Add(cookie);
            }
            catch (Exception ex)
            {
                // Log the error if you have logging available
                // Skip malformed cookie lines
                Console.WriteLine($"Failed to parse cookie line: {line}. Error: {ex.Message}");
            }
        }
        
        return cookies.AsReadOnly();
    }
    
    /// <summary>
    /// Parses cookies from a file path
    /// </summary>
    /// <param name="cookieFilePath">Path to the Netscape cookie file</param>
    /// <returns>A list of cookies that can be used with YoutubeExplode</returns>
    public static async Task<IReadOnlyList<Cookie>> ParseNetscapeCookiesFromFileAsync(string cookieFilePath)
    {
        var content = await File.ReadAllTextAsync(cookieFilePath);
        return ParseNetscapeCookies(content);
    }
}