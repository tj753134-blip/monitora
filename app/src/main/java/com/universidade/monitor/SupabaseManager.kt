// SupabaseManager.kt
package com.universidade.monitor

import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object SupabaseManager {
    private val properties: Properties by lazy {
        Properties().apply {
            val file = File("local.properties")
            if (file.exists()) {
                load(FileInputStream(file))
            }
        }
    }

    private val supabaseUrl: String by lazy {
        properties.getProperty("SUPABASE_URL", "").orEmpty()
    }

    private val supabaseKey: String by lazy {
        properties.getProperty("SUPABASE_ANON_KEY", "").orEmpty()
    }

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun connect() {
        Log.d("SupabaseManager", "Conectado ao canal de dados (modo local)")
    }

    fun disconnect() {
        Log.d("SupabaseManager", "Desconectado do canal de dados")
    }

    suspend fun broadcastData(channel: String, data: Map<String, Any>) {
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
            Log.d("SupabaseManager", "Configuração do Supabase ausente. Ignorando envio para $channel")
            return
        }

        Log.d("SupabaseManager", "Enviando para $channel via Supabase (placeholder compatível)")
    }
}