package com.alansouza.personaltasks.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alansouza.personaltasks.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonRegister: MaterialButton
    private lateinit var buttonBackToLogin: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        passwordInputLayout = findViewById(R.id.textInputLayoutPassword)
        confirmPasswordInputLayout = findViewById(R.id.textInputLayoutConfirmPassword)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin)
        progressBar = findViewById(R.id.progressBar)

        buttonRegister.setOnClickListener {
            // Limpa erros anteriores
            emailInputLayout.error = null
            passwordInputLayout.error = null
            confirmPasswordInputLayout.error = null

            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

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
            } else if (password.length < 6) {
                passwordInputLayout.error = "A senha deve ter pelo menos 6 caracteres"
                hasError = true
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordInputLayout.error = "Confirme sua senha"
                hasError = true
            } else if (password != confirmPassword) {
                confirmPasswordInputLayout.error = "As senhas não coincidem"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            progressBar.visibility = View.VISIBLE
            buttonRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    buttonRegister.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Conta criada com sucesso! Faça login para continuar.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    } else {
                        val errorMsg = when {
                            task.exception?.message?.contains("email address is already in use", ignoreCase = true) == true ->
                                "Este e-mail já está cadastrado."
                            task.exception?.message?.contains("badly formatted", ignoreCase = true) == true ->
                                "E-mail inválido."
                            else -> "Erro ao criar conta. Tente novamente."
                        }
                        emailInputLayout.error = errorMsg
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        buttonBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
