package com.example.projeto_narak

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: Int? = null,
    val nome: String,
    val repeticoes: Int,
    val series: Int,
    val usuario_id: String? = null  // Adicionado campo para o ID do usuário
)
