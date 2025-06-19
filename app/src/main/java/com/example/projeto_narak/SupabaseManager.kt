package com.example.projeto_narak

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.GoTrue
import android.util.Log

object SupabaseManager {
    private const val TAG = "SupabaseManager"
    
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://yjtjrvmceocbqlswxoyx.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqdGpydm1jZW9jYnFsc3d4b3l4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTAyMTgyNjAsImV4cCI6MjA2NTc5NDI2MH0.InR2WsZAHlWZS8GjK_cSz3K1brKln--4BRZg4ehFFZ8"
    ) {
        install(Postgrest)
        install(GoTrue)
        
        Log.d(TAG, "Supabase client inicializado")
    }
    
    // Método para testar a conexão
    suspend fun testConnection(): Boolean {
        return try {
            // Usar a propriedade postgrest do client
            val response = client.postgrest["usuarios"].select()
            Log.d(TAG, "Conexão com Supabase bem-sucedida")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao conectar com Supabase: ${e.message}", e)
            false
        }
    }
}
