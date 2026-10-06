package app.ergo.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local persistence. The OpenRouter key is encrypted with an AES key held in the
 * Android Keystore and never leaves the device. Backups are disabled in the manifest.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("ergo", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(K_KEY, null)?.let { runCatching { decrypt(it) }.getOrNull() }.orEmpty()
        set(v) {
            if (v.isEmpty()) prefs.edit().remove(K_KEY).apply()
            else prefs.edit().putString(K_KEY, encrypt(v)).apply()
        }

    var model: String
        get() = prefs.getString("model", null) ?: AUTO_MODEL
        set(v) = prefs.edit().putString("model", v).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()

    var goal: String?
        get() = prefs.getString("goal", null)
        set(v) = prefs.edit().putString("goal", v).apply()

    /** Learning progress as JSON (see [Progress]). The first version only knew about the Straw Man lesson. */
    var progress: Progress
        get() {
            val p = Progress.fromJson(prefs.getString("progress", null))
            return if (prefs.getBoolean("strawDone", false) && "straw_man" !in p.lessons) p.copy(lessons = p.lessons + "straw_man") else p
        }
        set(v) = prefs.edit().putString("progress", v.toJson()).remove("strawDone").apply()

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("themeMode", null) ?: "") }.getOrDefault(ThemeMode.System)
        set(v) = prefs.edit().putString("themeMode", v.name).apply()

    var markStyle: MarkStyle
        get() = runCatching { MarkStyle.valueOf(prefs.getString("markStyle", null) ?: "") }.getOrDefault(MarkStyle.Pencil)
        set(v) = prefs.edit().putString("markStyle", v.name).apply()

    var showCost: Boolean
        get() = prefs.getBoolean("showCost", true)
        set(v) = prefs.edit().putBoolean("showCost", v).apply()

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val c = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val out = c.doFinal(plain.toByteArray())
        return b64(c.iv) + ":" + b64(out)
    }

    private fun decrypt(stored: String): String {
        val (iv, data) = stored.split(":").let { it[0] to it[1] }
        val c = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        return String(c.doFinal(Base64.decode(data, Base64.NO_WRAP)))
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)

    private companion object {
        const val K_KEY = "orKey"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "ergo.orKey"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
