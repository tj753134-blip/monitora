package com.universidade.monitor

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CriptografiaManager {
    private const val CHAVE = "12345678901234567890123456789012"

    fun criptografar(dados: String): String {
        return try {
            val key = SecretKeySpec(CHAVE.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(dados.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            dados
        }
    }

    fun descriptografar(dadosCriptografados: String): String {
        return try {
            val key = SecretKeySpec(CHAVE.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decoded = Base64.decode(dadosCriptografados, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            dadosCriptografados
        }
    }
}
