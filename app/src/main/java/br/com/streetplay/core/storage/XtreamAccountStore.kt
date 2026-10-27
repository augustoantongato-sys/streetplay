package br.com.streetplay.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import br.com.streetplay.data.remote.XtreamCredentials
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Guarda usuário e senha do IPTV cifrados com uma chave protegida pelo Android Keystore. */
class XtreamAccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("street_play_xtream_secure", Context.MODE_PRIVATE)

    @Synchronized
    fun save(account: XtreamCredentials) {
        require(account.server.startsWith("https://") || account.server.startsWith("http://")) {
            "Informe um endereço HTTP ou HTTPS válido."
        }
        require(account.username.isNotBlank() && account.password.isNotBlank()) {
            "Preencha usuário e senha."
        }
        val plain = JSONObject()
            .put("server", account.server.trim().trimEnd('/'))
            .put("username", account.username.trim())
            .put("password", account.password)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain)
        prefs.edit()
            .putString(DATA_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    @Synchronized
    fun load(): XtreamCredentials? = runCatching {
        val data = prefs.getString(DATA_KEY, null) ?: return null
        val iv = Base64.decode(prefs.getString(IV_KEY, null) ?: return null, Base64.NO_WRAP)
        val encrypted = Base64.decode(data, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val json = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        XtreamCredentials(json.getString("server"), json.getString("username"), json.getString("password"))
    }.getOrNull()

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "street_play_xtream_credentials"
        const val DATA_KEY = "encrypted_account"
        const val IV_KEY = "account_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
