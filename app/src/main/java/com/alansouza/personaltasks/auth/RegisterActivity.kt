// Activity de registro de usuário, com validação de email, senha e confirmação.
package com.alansouza.personaltasks.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alansouza.personaltasks.R
import com.google.android.material.button.MaterialButton
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
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializa FirebaseAuth e views
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

        // Botão Criar conta: valida campos antes de registrar no Firebase
        buttonRegister.setOnClickListener {
            emailInputLayout.error = null
            passwordInputLayout.error = null
            confirmPasswordInputLayout.error = null

            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPwd = editTextConfirmPassword.text.toString().trim()

            var hasError = false
            if (email.isEmpty()) {
                emailInputLayout.error = "Digite seu e-mail"; hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.error = "E-mail inválido"; hasError = true
            }
            if (password.isEmpty()) {
                passwordInputLayout.error = "Digite sua senha"; hasError = true
            } else if (password.length < 6) {
                passwordInputLayout.error = "Senha deve ter ≥6 caracteres"; hasError = true
            }
            if (confirmPwd.isEmpty()) {
                confirmPasswordInputLayout.error = "Confirme sua senha"; hasError = true
            } else if (password != confirmPwd) {
                confirmPasswordInputLayout.error = "Senhas não coincidem"; hasError = true
            }
            if (hasError) return@setOnClickListener

            progressBar.visibility = View.VISIBLE
            buttonRegister.isEnabled = false

            // Cria usuário no FirebaseAuth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    buttonRegister.isEnabled = true
                    if (task.isSuccessful) {
                        // Sucesso: vai para Login
                        Toast.makeText(this, "Conta criada!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    } else {
                        // Erros específicos
                        val msg = when {
                            task.exception?.message?.contains("already in use", true) == true ->
                                "E-mail já cadastrado."
                            task.exception?.message?.contains("badly formatted", true) == true ->
                                "E-mail inválido."
                            else -> "Erro ao criar conta."
                        }
                        emailInputLayout.error = msg
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Botão voltar ao Login
        buttonBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
