package com.example.projeto_narak

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class ExerciseRepository {
    private val TAG = "ExerciseRepository"
    
    // Obter o cliente Supabase
    private val client = SupabaseManager.client
    // Acessar a tabela diretamente
    private val table = client.postgrest["exercicios"]

    suspend fun getAllExercises(userId: String): List<Exercise> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando exercícios para o usuário: $userId")
            
            // Primeiro, vamos tentar buscar sem filtro para ver se a tabela existe
            try {
                val allExercises = table.select().decodeList<Exercise>()
                Log.d(TAG, "Total de exercícios na tabela: ${allExercises.size}")
                
                // Filtrar manualmente pelo ID do usuário
                return@withContext allExercises.filter { it.usuario_id == userId }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar todos os exercícios: ${e.message}", e)
                
                // Tentar buscar sem filtro e sem decodificar para Exercise
                try {
                    val response = table.select()
                    val rawData = response.body.toString()
                    Log.d(TAG, "Dados brutos da tabela: $rawData")
                    
                    // Tentar decodificar como um mapa genérico
                    val maps = response.decodeList<Map<String, Any>>()
                    Log.d(TAG, "Exercícios como mapas: ${maps.size}")
                    
                    if (maps.isNotEmpty()) {
                        val firstItem = maps.first()
                        Log.d(TAG, "Primeiro item na tabela: $firstItem")
                        Log.d(TAG, "Campos disponíveis: ${firstItem.keys.joinToString()}")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Erro ao buscar dados brutos: ${e2.message}", e2)
                }
                
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar exercícios: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun insertExercise(ex: Exercise): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Inserindo exercício: ${ex.nome} para usuário: ${ex.usuario_id}")
            
            // Verificar se a tabela existe e tem a estrutura correta
            try {
                val response = table.select()
                val maps = response.decodeList<Map<String, Any>>()
                
                if (maps.isNotEmpty()) {
                    val firstItem = maps.first()
                    Log.d(TAG, "Campos disponíveis na tabela: ${firstItem.keys.joinToString()}")
                    
                    // Verificar se o campo usuario_id existe
                    if (!firstItem.containsKey("usuario_id")) {
                        Log.d(TAG, "Campo usuario_id não existe na tabela, tentando criar")
                        // Não podemos criar a coluna aqui, precisamos fazer isso no Supabase
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao verificar estrutura da tabela: ${e.message}", e)
            }
            
            // Tentar inserir o exercício
            table.insert(ex)
            Log.d(TAG, "Exercício inserido com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir exercício: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateExercise(ex: Exercise): Unit = withContext(Dispatchers.IO) {
        if (ex.id != null) {
            try {
                Log.d(TAG, "Atualizando exercício ID: ${ex.id}, Nome: ${ex.nome}")
                // Criar um novo objeto Exercise sem o ID para atualização
                val exerciseUpdate = Exercise(
                    id = null,  // Não incluir o ID na atualização
                    nome = ex.nome,
                    repeticoes = ex.repeticoes,
                    series = ex.series,
                    usuario_id = ex.usuario_id  // Manter o ID do usuário
                )
                
                // Usar a sintaxe de filtro com o método eq
                table.update(exerciseUpdate) {
                    eq("id", ex.id)
                }
                Log.d(TAG, "Exercício atualizado com sucesso")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar exercício: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun deleteExercise(id: Int): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Excluindo exercício ID: $id")
            // Usar a sintaxe de filtro com o método eq
            table.delete {
                eq("id", id)
            }
            Log.d(TAG, "Exercício excluído com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir exercício: ${e.message}", e)
            throw e
        }
    }
}