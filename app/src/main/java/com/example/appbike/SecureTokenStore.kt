package com.example.appbike

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object SecureTokenStore {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "appbike_api_access_token"
    private const val PREFS = "appbike_secure_session"
    private const val TOKEN = "access_token"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun save(context: Context, token: String) {
        if (token.isBlank()) {
            clear(context)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(1 + cipher.iv.size + encrypted.size).also { output ->
            output[0] = cipher.iv.size.toByte()
            cipher.iv.copyInto(output, 1)
            encrypted.copyInto(output, 1 + cipher.iv.size)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context): String {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN, null) ?: return ""
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val ivSize = packed.first().toInt() and 0xFF
            require(ivSize in 12..32 && packed.size > 1 + ivSize)
            val iv = packed.copyOfRange(1, 1 + ivSize)
            val encrypted = packed.copyOfRange(1 + ivSize, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            }
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrElse {
            clear(context)
            ""
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(TOKEN)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }
}
