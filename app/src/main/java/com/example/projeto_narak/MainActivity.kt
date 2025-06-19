package com.example.projeto_narak

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.util.Log

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val repository = ExerciseRepository()
    private lateinit var listView: ListView
    private lateinit var adapter: ExerciseAdapter
    private var exercises = mutableListOf<Exercise>()
    private lateinit var progressBar: ProgressBar
    private var editingExerciseId: Int? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sessionManager = SessionManager(this)
        
        // Verificar se o usuário está logado
        if (!sessionManager.isLoggedIn()) {
            // Redirecionar para a tela de login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        val userId = sessionManager.getUserId()
        val userEmail = sessionManager.getUserEmail()
        
        // Configurar título da ActionBar
        supportActionBar?.title = "Exercícios de $userEmail"

        listView = findViewById(R.id.exerciseList)
        progressBar = findViewById(R.id.progressBar)
        val nome = findViewById<EditText>(R.id.editNome)
        val repeticoes = findViewById<EditText>(R.id.editReps)
        val series = findViewById<EditText>(R.id.editSeries)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        adapter = ExerciseAdapter()
        listView.adapter = adapter
        
        // Verificar e configurar o banco de dados
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val setup = DatabaseSetup()
                val success = setup.checkAndSetupTables()
                
                if (!success) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, 
                            "Atenção: A tabela de exercícios não tem a coluna usuario_id. " +
                            "Por favor, adicione esta coluna no Supabase.", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao configurar banco de dados", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }

        btnAdd.setOnClickListener {
            if (nome.text.isNotEmpty()) {
                val exercicio = Exercise(
                    id = editingExerciseId,
                    nome = nome.text.toString(),
                    repeticoes = repeticoes.text.toString().toIntOrNull() ?: 0,
                    series = series.text.toString().toIntOrNull() ?: 0,
                    usuario_id = userId  // Adicionar o ID do usuário
                )
                
                progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        if (editingExerciseId == null) {
                            repository.insertExercise(exercicio)
                        } else {
                            repository.updateExercise(exercicio)
                            editingExerciseId = null
                            btnAdd.text = "Adicionar exercício"
                        }
                        nome.text.clear()
                        repeticoes.text.clear()
                        series.text.clear()
                        carregarExercicios()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }
            } else {
                Toast.makeText(this, "Nome do exercício é obrigatório", Toast.LENGTH_SHORT).show()
            }
        }

        carregarExercicios()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Fazer logout
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun carregarExercicios() {
        val userId = sessionManager.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Erro: ID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                exercises = repository.getAllExercises(userId).toMutableList()
                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    if (exercises.isEmpty()) {
                        Toast.makeText(this@MainActivity, 
                            "Nenhum exercício encontrado. Adicione um novo!", 
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar exercícios", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, 
                        "Erro ao carregar: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun editarExercicio(exercise: Exercise) {
        editingExerciseId = exercise.id
        findViewById<EditText>(R.id.editNome).setText(exercise.nome)
        findViewById<EditText>(R.id.editReps).setText(exercise.repeticoes.toString())
        findViewById<EditText>(R.id.editSeries).setText(exercise.series.toString())
        findViewById<Button>(R.id.btnAdd).text = "Atualizar exercício"
    }

    private fun excluirExercicio(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar exclusão")
            .setMessage("Tem certeza que deseja excluir este exercício?")
            .setPositiveButton("Sim") { _, _ ->
                progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        repository.deleteExercise(id)
                        carregarExercicios()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    inner class ExerciseAdapter : BaseAdapter() {
        override fun getCount(): Int = exercises.size

        override fun getItem(position: Int): Any = exercises[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_exercise, parent, false)
            val exercise = exercises[position]
            
            val textExercise = view.findViewById<TextView>(R.id.textExercise)
            val btnEdit = view.findViewById<Button>(R.id.btnEdit)
            val btnDelete = view.findViewById<Button>(R.id.btnDelete)
            
            textExercise.text = "${exercise.nome} - ${exercise.repeticoes}x${exercise.series}"
            
            btnEdit.setOnClickListener {
                editarExercicio(exercise)
            }
            
            btnDelete.setOnClickListener {
                exercise.id?.let { id -> excluirExercicio(id) }
            }
            
            return view
        }
    }
}