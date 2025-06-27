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

        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        passwordInputLayout = findViewById(R.id.textInputLayoutPassword)
        auth = FirebaseAuth.getInstance()
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        progressBar = findViewById(R.id.progressBar)
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            // Limpa erros anteriores
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

            progressBar.visibility = View.VISIBLE
            buttonLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    buttonLogin.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finishAffinity()
                    } else {
                        val errorMsg = when (task.exception?.message) {
                            "The password is invalid or the user does not have a password." ->
                                "Senha incorreta. Tente novamente."
                            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                "Usuário não encontrado. Verifique o e-mail digitado."
                            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                                "Erro de conexão. Verifique sua internet."
                            else -> "Erro ao fazer login. Verifique seus dados."
                        }
                        passwordInputLayout.error = errorMsg
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}