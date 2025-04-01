package org.porebazu.bot.chrisalaxelrto.identity

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect
import com.microsoft.aad.msal4j.ITokenCacheAccessContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SafeTokenCacheAccessAspect : ITokenCacheAccessAspect {
    init {
        // Ensure the cache file and secret key file exist
        try {
            if (!Files.exists(Paths.get(TMP_FOLDER_PATH))) {
                Files.createDirectory(Paths.get(TMP_FOLDER_PATH))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun beforeCacheAccess(context: ITokenCacheAccessContext) {
        val cacheData = loadCache()
        context.tokenCache().deserialize(cacheData)
    }

    override fun afterCacheAccess(context: ITokenCacheAccessContext) {
        if (context.hasCacheChanged()) {
            val cacheData = context.tokenCache().serialize()
            saveCache(cacheData)
        }
    }

    private fun loadCache(): String {
        return try {
            if (!Files.exists(Paths.get(CACHE_FILE_PATH))) {
                Files.createFile(Paths.get(CACHE_FILE_PATH))
            }
            val encryptedData = Files.readAllBytes(Paths.get(CACHE_FILE_PATH))
            val decryptedData = decrypt(encryptedData)
            String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun saveCache(cacheData: String) {
        try {
            val encryptedData = encrypt(cacheData.toByteArray())
            Files.write(
                Paths.get(CACHE_FILE_PATH),
                encryptedData,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val secretKey: SecretKey = SecretKeySpec(secretKey, ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    @Throws(Exception::class)
    private fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val secretKey: SecretKey = SecretKeySpec(secretKey, ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    @get:Throws(Exception::class)
    private val secretKey: ByteArray
        private get() {
            val secretPath = Paths.get(SECRET_FILE_PATH)
            return if (Files.exists(secretPath)) {
                Files.readAllBytes(secretPath)
            } else {
                val secretKey = generateRandomSecret()
                Files.write(secretPath, secretKey, StandardOpenOption.CREATE)
                secretKey
            }
        }

    private fun generateRandomSecret(): ByteArray {
        val random = SecureRandom()
        val secretBytes = ByteArray(SECRET_KEY_SIZE)
        random.nextBytes(secretBytes)
        return secretBytes
    }

    companion object {
        private const val TMP_FOLDER_PATH = "./tmp"
        private const val CACHE_FILE_PATH = "$TMP_FOLDER_PATH/cache.dat"
        private const val SECRET_FILE_PATH = "$TMP_FOLDER_PATH/secret.key"
        private const val ENCRYPTION_ALGORITHM = "AES"
        private const val SECRET_KEY_SIZE = 32 // 256 bits
    }
}