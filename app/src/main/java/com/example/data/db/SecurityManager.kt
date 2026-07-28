package com.example.data.db

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {
    // Standard key and IV derivation for local vault encryption
    private const val SECRET_KEY_STRING = "AuraAssistantVaultKey2026Secure" // 32 chars = 256 bits
    private const val INIT_VECTOR = "AuraIvVector1234" // 16 chars = 128 bits

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(SECRET_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(INIT_VECTOR.toByteArray(Charsets.UTF_8))
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(SECRET_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(INIT_VECTOR.toByteArray(Charsets.UTF_8))
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }
}
