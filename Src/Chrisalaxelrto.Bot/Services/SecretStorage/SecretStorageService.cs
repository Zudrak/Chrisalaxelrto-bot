// Src/Chrisalaxelrto.Bot/Services/SecretStorageService.cs
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace Chrisalaxelrto.Bot.Services;

public class SecretStorageService
{
    private readonly string secretsFilePath;
    private readonly string keyFilePath;
    private bool areAllSecretsLoaded = true;
    private Dictionary<string, string> secretsCache = new Dictionary<string, string>();

    private List<string> requiredSecrets = new List<string>
    {
        "BotToken"
    };

    private List<string> missingSecrets = new List<string>();

    public SecretStorageService(string? storageDirectory = null)
    {
        var baseDir = storageDirectory ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "Chrisalaxelrto");

        Directory.CreateDirectory(baseDir);

        secretsFilePath = Path.Combine(baseDir, "secrets.enc");
        keyFilePath = Path.Combine(baseDir, ".key");
        secretsCache = LoadSecrets();

        // Check if all required secrets are loaded
        foreach (var secretKey in requiredSecrets)
        {
            if (!secretsCache.ContainsKey(secretKey))
            {
                missingSecrets.Add(secretKey);
                areAllSecretsLoaded = false;
                break;
            }
        }
    }
    
    public void RequestSecretsFromConsole()
    {
        Console.Write($"Welcome to the secret storage setup! Please enter the following secrets:\n");

        foreach (var secretKey in missingSecrets)
        {
            Console.Write($"{secretKey}: ");
            var value = Console.ReadLine();
            if (!string.IsNullOrEmpty(value))
            {
                secretsCache[secretKey] = value;
            }
            else
            {
                throw new InvalidOperationException($"Secret '{secretKey}' cannot be empty.");
            }
        }

        SaveSecrets(secretsCache);
        missingSecrets.Clear();
        areAllSecretsLoaded = true;
    }

    public string? GetSecret(string key)
    {
        var value = secretsCache.GetValueOrDefault(key);

        if(value == null)
        {
            areAllSecretsLoaded = false;
        }

        return value;
    }

    public bool HasSecret(string key)
    {
        return secretsCache.ContainsKey(key);
    }

    public bool AreAllSecretsLoaded() => areAllSecretsLoaded;

    public IEnumerable<string> GetMissingSecrets() => missingSecrets;

    private Dictionary<string, string> LoadSecrets()
    {
        if (!File.Exists(secretsFilePath))
            return new Dictionary<string, string>();

        try
        {
            var encryptedData = File.ReadAllBytes(secretsFilePath);
            var key = GetOrCreateKey();
            var decryptedJson = Decrypt(encryptedData, key);
            return JsonSerializer.Deserialize<Dictionary<string, string>>(decryptedJson)
                ?? new Dictionary<string, string>();
        }
        catch
        {
            return new Dictionary<string, string>();
        }
    }

    private void SaveSecrets(Dictionary<string, string> secrets)
    {
        var json = JsonSerializer.Serialize(secrets);
        var key = GetOrCreateKey();
        var encrypted = Encrypt(json, key);
        File.WriteAllBytes(secretsFilePath, encrypted);
    }

    private byte[] GetOrCreateKey()
    {
        if (File.Exists(keyFilePath))
        {
            return File.ReadAllBytes(keyFilePath);
        }

        var key = RandomNumberGenerator.GetBytes(32); // 256-bit key
        File.WriteAllBytes(keyFilePath, key);
        
        // Set file permissions to user-only (Unix-like systems)
        if (!OperatingSystem.IsWindows())
        {
            File.SetUnixFileMode(keyFilePath, 
                UnixFileMode.UserRead | UnixFileMode.UserWrite);
        }
        
        return key;
    }

    private static byte[] Encrypt(string plainText, byte[] key)
    {
        using var aes = Aes.Create();
        aes.Key = key;
        aes.GenerateIV();

        using var encryptor = aes.CreateEncryptor();
        var plainBytes = Encoding.UTF8.GetBytes(plainText);
        var cipherBytes = encryptor.TransformFinalBlock(plainBytes, 0, plainBytes.Length);

        // Prepend IV to cipher text
        var result = new byte[aes.IV.Length + cipherBytes.Length];
        Buffer.BlockCopy(aes.IV, 0, result, 0, aes.IV.Length);
        Buffer.BlockCopy(cipherBytes, 0, result, aes.IV.Length, cipherBytes.Length);

        return result;
    }

    private static string Decrypt(byte[] encryptedData, byte[] key)
    {
        using var aes = Aes.Create();
        aes.Key = key;

        // Extract IV from the beginning
        var iv = new byte[aes.IV.Length];
        var cipherBytes = new byte[encryptedData.Length - iv.Length];
        
        Buffer.BlockCopy(encryptedData, 0, iv, 0, iv.Length);
        Buffer.BlockCopy(encryptedData, iv.Length, cipherBytes, 0, cipherBytes.Length);

        aes.IV = iv;

        using var decryptor = aes.CreateDecryptor();
        var plainBytes = decryptor.TransformFinalBlock(cipherBytes, 0, cipherBytes.Length);
        
        return Encoding.UTF8.GetString(plainBytes);
    }
}