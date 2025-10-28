// Src/Chrisalaxelrto.Bot/Services/SecretConfigurationProvider.cs
using Microsoft.Extensions.Configuration;

namespace Chrisalaxelrto.Bot.Services;

public class SecretConfigurationProvider : ConfigurationProvider
{
    private readonly SecretStorageService _secretStorage;

    public SecretConfigurationProvider(SecretStorageService secretStorage)
    {
        _secretStorage = secretStorage;
    }

    public override void Load()
    {
        // Try to load common secrets
        var botToken = _secretStorage.GetSecret("BotToken");
        if (!string.IsNullOrEmpty(botToken))
        {
            Data["BotToken"] = botToken;
        }

        var youtubeCookies = _secretStorage.GetSecret("YouTubeCookies");
        if (!string.IsNullOrEmpty(youtubeCookies))
        {
            Data["YouTubeCookies"] = youtubeCookies;
        }
    }
}

public class SecretConfigurationSource : IConfigurationSource
{
    private readonly SecretStorageService _secretStorage;

    public SecretConfigurationSource(SecretStorageService secretStorage)
    {
        _secretStorage = secretStorage;
    }

    public IConfigurationProvider Build(IConfigurationBuilder builder)
    {
        return new SecretConfigurationProvider(_secretStorage);
    }
}

public static class SecretConfigurationExtensions
{
    public static IConfigurationBuilder AddSecretStorage(
        this IConfigurationBuilder builder, 
        SecretStorageService secretStorage)
    {
        return builder.Add(new SecretConfigurationSource(secretStorage));
    }
}