package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context) {
    private var keyStore: KeyStore? = null
    private var fallbackKey: SecretKey? = null

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
        } catch (e: Throwable) {
            keyStore = null
        }

        // Always prepare standard fallback key in SharedPreferences just in case the AndroidKeyStore throws or returns null
        try {
            val sharedPrefs = context.getSharedPreferences("secure_key_fallback", Context.MODE_PRIVATE)
            val storedBase64Key = sharedPrefs.getString("aes_fallback_key", null)
            val finalKey = if (storedBase64Key == null) {
                // Generate secure random AES-256 key
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                val base64Key = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                sharedPrefs.edit().putString("aes_fallback_key", base64Key).apply()
                randomBytes
            } else {
                Base64.decode(storedBase64Key, Base64.NO_WRAP)
            }
            fallbackKey = SecretKeySpec(finalKey, "AES")
        } catch (e: Throwable) {
            // Ultimate fallback in memory
            fallbackKey = SecretKeySpec(ByteArray(32), "AES")
        }

        // Proactive KeyStore pre-flight check to verify that key generation and cipher init are fully functional without any OS/native crashes
        if (keyStore != null) {
            try {
                val rawKey = getKey()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = ByteArray(12)
                SecureRandom().nextBytes(iv)
                cipher.init(Cipher.ENCRYPT_MODE, rawKey, GCMParameterSpec(128, iv))
                cipher.doFinal("test_handshake".toByteArray(Charsets.UTF_8))
            } catch (t: Throwable) {
                // Discard KeyStore if it behaves unexpectedly or fails compatibility
                keyStore = null
            }
        }
    }

    private fun getKey(): SecretKey {
        val ks = keyStore
        if (ks != null) {
            try {
                val existingKey = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                existingKey?.secretKey?.let { return it }
                
                // Try creating
                return createKey()
            } catch (e: Throwable) {
                // Fallback to our robust local software-level AES key in database
            }
        }
        return fallbackKey ?: SecretKeySpec(ByteArray(32), "AES")
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(bytes)

        // Structure: Length of IV (4 bytes) + IV Bytes + Encrypted Bytes
        val buffer = ByteBuffer.allocate(4 + iv.size + encrypted.size)
            .putInt(iv.size)
            .put(iv)
            .put(encrypted)
        return buffer.array()
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(bytes)
        val ivSize = buffer.int
        if (ivSize <= 0 || ivSize > 100) {
            throw IllegalArgumentException("Invalid IV size in encrypted data")
        }
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    fun encryptString(text: String): String {
        return try {
            val encryptedBytes = encrypt(text.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            ""
        }
    }

    fun decryptString(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            String(decrypt(decodedBytes), Charsets.UTF_8)
        } catch (e: Throwable) {
            "Decryption Error"
        }
    }

    companion object {
        private const val KEY_ALIAS = "expense_encryption_secure_key"
    }
}
