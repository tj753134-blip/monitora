// SupabaseManager.kt
package com.universidade.monitor

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.broadcast
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
        properties.getProperty("SUPABASE_URL")
    }

    private val supabaseKey: String by lazy {
        properties.getProperty("SUPABASE_ANON_KEY")
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl, supabaseKey) {
            install(GoTrue)
            install(Postgrest)
            install(Realtime)
        }
    }
    
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    suspend fun broadcastData(channel: String, data: Map<String, Any>) {
        client.realtime.broadcast(channel) {
            send("data", data)
        }
    }
}