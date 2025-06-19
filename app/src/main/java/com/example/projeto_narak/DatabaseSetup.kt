package com.example.projeto_narak

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.postgrest

class DatabaseSetup {
    private val TAG = "DatabaseSetup"
    private val client = SupabaseManager.client
    
    suspend fun checkAndSetupTables(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Verificando e configurando tabelas")
            
            // Verificar tabela de exercícios
            val exerciciosTable = client.postgrest["exercicios"]
            try {
                val response = exerciciosTable.select()
                val maps = response.decodeList<Map<String, Any>>()
                
                if (maps.isNotEmpty()) {
                    val firstItem = maps.first()
                    Log.d(TAG, "Campos disponíveis na tabela exercicios: ${firstItem.keys.joinToString()}")
                    
                    // Verificar se o campo usuario_id existe
                    if (!firstItem.containsKey("usuario_id")) {
                        Log.d(TAG, "Campo usuario_id não existe na tabela exercicios")
                        // Não podemos criar a coluna aqui, precisamos fazer isso no Supabase
                        return@withContext false
                    }
                } else {
                    Log.d(TAG, "Tabela exercicios está vazia")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao verificar tabela exercicios: ${e.message}", e)
                return@withContext false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar banco de dados: ${e.message}", e)
            false
        }
    }
}