package com.example.projeto_narak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private val userRepository = UserRepository()
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        sessionManager = SessionManager(this)
        
        // Verificar se o usuário já está logado
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "Usuário já está logado, redirecionando para MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val emailInput = findViewById<EditText>(R.id.editEmail)
        val senhaInput = findViewById<EditText>(R.id.editSenha)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        // Adicionar um botão para criar um usuário de teste
        val btnCreateUser = findViewById<Button>(R.id.btnCreateUser)
        btnCreateUser?.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val inserted = userRepository.insertTestUser()
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, 
                            if (inserted) "Usuário de teste criado com sucesso" 
                            else "Falha ao criar usuário de teste", 
                            Toast.LENGTH_SHORT).show()
                        
                        // Preencher os campos com os dados do usuário de teste
                        emailInput.setText("teste@exemplo.com")
                        senhaInput.setText("senha123")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao criar usuário de teste", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
        
        // Adicionar um botão para criar um usuário com senha em texto simples
        val btnCreatePlainUser = findViewById<Button>(R.id.btnCreatePlainUser)
        btnCreatePlainUser?.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val inserted = userRepository.insertPlainTextUser()
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, 
                            if (inserted) "Usuário plain criado com sucesso" 
                            else "Falha ao criar usuário plain", 
                            Toast.LENGTH_SHORT).show()
                        
                        // Preencher os campos com os dados do usuário plain
                        emailInput.setText("plain@exemplo.com")
                        senhaInput.setText("senha123")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao criar usuário plain", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val senha = senhaInput.text.toString()
            
            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Log.d(TAG, "Tentando login com email: $email")
            progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Iniciando validação de credenciais")
                    val user = userRepository.getUserByEmailAndPassword(email, senha)
                    
                    if (user != null) {
                        Log.d(TAG, "Login bem-sucedido, criando sessão")
                        // Criar sessão para o usuário
                        sessionManager.createLoginSession(user.id ?: "", user.email)
                        
                        Log.d(TAG, "Iniciando MainActivity")
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.d(TAG, "Credenciais inválidas")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Credenciais inválidas", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao fazer login", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Erro ao fazer login: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Testar a conexão com o Supabase e verificar tabelas
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Verificando conexão com Supabase")
                val connected = SupabaseManager.testConnection()
                Log.d(TAG, "Teste de conexão: $connected")
                
                if (connected) {
                    // Verificar a estrutura exata da tabela
                    try {
                        val tableStructure = userRepository.checkExactTableStructure()
                        Log.d(TAG, "Estrutura da tabela: $tableStructure")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao verificar estrutura exata da tabela", e)
                    }
                    
                    // Listar todos os usuários
                    try {
                        Log.d(TAG, "Tentando listar todos os usuários")
                        val users = userRepository.listAllUsers()
                        Log.d(TAG, "Usuários encontrados: ${users.size}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao listar usuários", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao testar conexão", e)
            }
        }
    }
}