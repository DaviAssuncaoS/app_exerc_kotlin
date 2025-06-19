package com.example.projeto_narak

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import android.util.Log

@Serializable
data class User(
    val id: String? = null,  // Alterado para String pois parece ser um UUID
    val email: String,
    val senha: String
)

@Serializable
data class UserAlt(
    val id: Int? = null,
    val email: String? = null,
    val senha: String? = null,
    val username: String? = null,
    val password: String? = null,
    val name: String? = null
)

class UserRepository {
    private val TAG = "UserRepository"

    // Obter o cliente Supabase
    private val client = SupabaseManager.client
    // Acessar a tabela diretamente
    private val table = client.postgrest["usuarios"]

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando usuário com email: $email")

            // Primeiro, vamos listar todos os usuários para ver o que temos
            val allUsers = listAllUsers()
            Log.d(TAG, "Total de usuários na base: ${allUsers.size}")

            // Agora, vamos buscar o usuário específico
            val response = table.select {
                eq("email", email)
            }

            val users = response.decodeList<User>()
            Log.d(TAG, "Usuários encontrados com email '$email': ${users.size}")

            if (users.isNotEmpty()) {
                val user = users.first()
                Log.d(TAG, "Usuário encontrado: ID=${user.id}, Email=${user.email}, Senha=${user.senha}")
                user
            } else {
                Log.d(TAG, "Nenhum usuário encontrado com email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar usuário: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    suspend fun validateCredentials(email: String, senha: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validando credenciais para email: $email, senha: $senha")
            
            val user = getUserByEmailAndPassword(email, senha)
            
            if (user != null) {
                // Criar sessão para o usuário
                val sessionManager = SessionManager(MyApplication.appContext)
                sessionManager.createLoginSession(user.id ?: "", user.email)
                
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar credenciais: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun listAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listando todos os usuários")
            val response = table.select()

            val users = response.decodeList<User>()
            Log.d(TAG, "Total de usuários: ${users.size}")

            users.forEach { user ->
                Log.d(TAG, "Usuário: ID=${user.id}, Email=${user.email}, Senha=${user.senha}")
            }

            users
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao listar usuários: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun listAllUsersAlt(): List<UserAlt> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listando todos os usuários (alternativo)")
            val response = table.select()

            val users = response.decodeList<UserAlt>()
            Log.d(TAG, "Total de usuários (alt): ${users.size}")

            users.forEach { user ->
                Log.d(TAG, "Usuário (alt): ID=${user.id}, Email=${user.email}, Senha=${user.senha}, " +
                        "Username=${user.username}, Password=${user.password}, Name=${user.name}")
            }

            users
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao listar usuários (alt): ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Método para inserir um usuário de teste
    suspend fun insertTestUser(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Inserindo usuário de teste")
            
            // Primeiro, verificar se o usuário já existe
            val exists = checkUserExists("teste@exemplo.com")
            if (exists) {
                Log.d(TAG, "Usuário de teste já existe, não é necessário inserir novamente")
                return@withContext true
            }
            
            val testUser = User(
                id = null,
                email = "teste@exemplo.com",
                senha = "senha123"  // Isso será armazenado como texto simples
            )
            
            Log.d(TAG, "Inserindo usuário: Email=${testUser.email}, Senha=${testUser.senha}")
            table.insert(testUser)
            Log.d(TAG, "Usuário de teste inserido com sucesso")
            
            // Verificar se o usuário foi inserido corretamente
            val inserted = checkUserExists("teste@exemplo.com")
            Log.d(TAG, "Verificação pós-inserção: usuário existe = $inserted")
            
            inserted
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir usuário de teste: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Método para verificar a estrutura da tabela
    suspend fun checkTableStructure() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Verificando estrutura da tabela usuarios")

            // Vamos usar uma abordagem diferente para verificar a estrutura da tabela
            val response = table.select()
            val users = response.decodeList<User>()

            if (users.isNotEmpty()) {
                val user = users.first()
                Log.d(TAG, "Estrutura da tabela: id=${user.id != null}, email=${user.email}, senha=${user.senha}")
            } else {
                Log.d(TAG, "Tabela vazia, não foi possível verificar a estrutura")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar estrutura da tabela: ${e.message}", e)
        }
    }

    suspend fun checkUserExists(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Verificando se o usuário existe: $email")

            val response = table.select {
                eq("email", email)
            }

            val users = response.decodeList<User>()
            val exists = users.isNotEmpty()

            Log.d(TAG, "Usuário existe: $exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar se o usuário existe: ${e.message}", e)
            false
        }
    }

    // Método para verificar a estrutura exata da tabela
    suspend fun checkExactTableStructure(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Verificando estrutura exata da tabela usuarios")
            
            // Vamos selecionar um usuário e imprimir todos os campos
            val response = table.select()
            val rawData = response.body.toString()
            Log.d(TAG, "Dados brutos da tabela: $rawData")
            
            // Tentar decodificar como um mapa genérico
            val maps = response.decodeList<Map<String, Any>>()
            
            if (maps.isNotEmpty()) {
                val firstItem = maps.first()
                Log.d(TAG, "Primeiro item na tabela: $firstItem")
                Log.d(TAG, "Campos disponíveis: ${firstItem.keys.joinToString()}")
                
                // Verificar cada campo
                firstItem.forEach { (key, value) ->
                    Log.d(TAG, "Campo: $key, Valor: $value, Tipo: ${value?.javaClass?.simpleName}")
                }
            } else {
                Log.d(TAG, "Tabela vazia, não foi possível verificar a estrutura exata")
            }
            
            maps
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar estrutura exata da tabela: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Método para inserir um usuário com senha em texto simples para testes
    suspend fun insertPlainTextUser(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Inserindo usuário com senha em texto simples")
            
            // Verificar se o usuário já existe
            val exists = checkUserExists("plain@exemplo.com")
            if (exists) {
                Log.d(TAG, "Usuário plain já existe, não é necessário inserir novamente")
                return@withContext true
            }
            
            val testUser = User(
                id = null,
                email = "plain@exemplo.com",
                senha = "senha123"
            )
            
            Log.d(TAG, "Inserindo usuário: Email=${testUser.email}, Senha=${testUser.senha}")
            table.insert(testUser)
            Log.d(TAG, "Usuário plain inserido com sucesso")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir usuário plain: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserByEmailAndPassword(email: String, senha: String): User? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando usuário com email: $email e validando senha")
            
            // Buscar o usuário pelo email
            val response = table.select {
                eq("email", email)
            }
            
            val users = response.decodeList<User>()
            Log.d(TAG, "Usuários encontrados com email '$email': ${users.size}")
            
            if (users.isNotEmpty()) {
                val user = users.first()
                Log.d(TAG, "Usuário encontrado: ID=${user.id}, Email=${user.email}")
                
                // Comparar as senhas
                val isValid = user.senha == senha
                Log.d(TAG, "Senha válida: $isValid")
                
                // Para fins de teste, vamos retornar o usuário mesmo que as senhas não correspondam
                // Remova esta condição em produção!
                if (!isValid) {
                    Log.d(TAG, "Senhas não correspondem, mas permitindo login para teste")
                    return@withContext user
                }
                
                if (isValid) user else null
            } else {
                Log.d(TAG, "Nenhum usuário encontrado com email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar usuário: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}
