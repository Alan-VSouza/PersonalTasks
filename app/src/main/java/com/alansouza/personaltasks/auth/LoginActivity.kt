// Activity de Login com validação de email/senha e botões para cadastro.
package com.alansouza.personaltasks.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alansouza.personaltasks.MainActivity
import com.alansouza.personaltasks.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var emailInputLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var passwordInputLayout: com.google.android.material.textfield.TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa FirebaseAuth e referências das views
        auth = FirebaseAuth.getInstance()
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        passwordInputLayout = findViewById(R.id.textInputLayoutPassword)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        progressBar = findViewById(R.id.progressBar)

        // Botão Entrar: valida campos e tenta login por email/senha
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            emailInputLayout.error = null
            passwordInputLayout.error = null

            var hasError = false
            if (email.isEmpty()) {
                emailInputLayout.error = "Digite seu e-mail"
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.error = "E-mail inválido"
                hasError = true
            }
            if (password.isEmpty()) {
                passwordInputLayout.error = "Digite sua senha"
                hasError = true
            }
            if (hasError) return@setOnClickListener

            // Exibe ProgressBar durante autenticação
            progressBar.visibility = View.VISIBLE
            buttonLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    buttonLogin.isEnabled = true
                    if (task.isSuccessful) {
                        // Navega para MainActivity em caso de sucesso
                        startActivity(Intent(this, MainActivity::class.java))
                        finishAffinity()
                    } else {
                        // Mostra mensagem de erro personalizada
                        val msg = when (task.exception?.message) {
                            "The password is invalid or the user does not have a password." ->
                                "Senha incorreta. Tente novamente."
                            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                "Usuário não encontrado."
                            "A network error (such as timeout...)" ->
                                "Erro de conexão. Verifique sua internet."
                            else -> "Erro ao fazer login."
                        }
                        passwordInputLayout.error = msg
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Botão Criar conta: abre RegisterActivity
        buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Se já estiver logado, vai direto para MainActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
