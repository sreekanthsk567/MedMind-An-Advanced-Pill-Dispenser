package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebug) {
            Log.i("LoginActivity", "Initializing AppCheck DEBUG provider")
            val debugFactory = DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugFactory)

        } else {
            Log.i("LoginActivity", "Initializing AppCheck Play Integrity provider")
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        auth = Firebase.auth
        if (auth.currentUser != null) {
            Log.d("LoginActivity", "User already logged in: ${auth.currentUser?.uid}")
            showLoading(true)
            fetchUserRoleAndNavigate(auth.currentUser!!.uid)
        } else {
            showLoading(false)
        }
        val roles = arrayOf(
            getString(R.string.role_patient),
            getString(R.string.role_caregiver),
            getString(R.string.role_doctor)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spinnerRole.adapter = adapter
        binding.btnToggleLogin.setOnClickListener {
            binding.groupLogin.visibility = View.GONE
            binding.btnToggleLogin.visibility = View.GONE
            binding.groupRegister.visibility = View.VISIBLE
            binding.btnToggleRegister.visibility = View.VISIBLE
        }
        binding.btnToggleRegister.setOnClickListener {
            binding.groupLogin.visibility = View.VISIBLE
            binding.btnToggleLogin.visibility = View.VISIBLE
            binding.groupRegister.visibility = View.GONE
            binding.btnToggleRegister.visibility = View.GONE
        }
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Login successful")
                        fetchUserRoleAndNavigate(auth.currentUser!!.uid)
                    } else {
                        Log.w("LoginActivity", "Login failed", task.exception)
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showLoading(false)
                    }
                }
        }
        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()
            val selectedRole = when (binding.spinnerRole.selectedItemPosition) {
                1 -> "Caregiver"
                2 -> "Doctor"
                else -> "Patient"
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Registration successful")
                        val uid = auth.currentUser!!.uid

                        val newUser = User(
                            uid = uid,
                            email = email,
                            fullName = name,
                            role = selectedRole,
                            pillsChamber1 = 0,
                            pillsChamber2 = 0,
                            patientIds = emptyList(),
                            caregiverId = "NOT_SET"
                        )

                        val db = Firebase.firestore
                        db.collection("users").document(uid)
                            .set(newUser)
                            .addOnSuccessListener {
                                Log.d("LoginActivity", "User profile created in Firestore")
                                navigateToScreen(selectedRole)
                            }
                            .addOnFailureListener { e ->
                                Log.w("LoginActivity", "Error creating user profile", e)
                                Toast.makeText(
                                    this,
                                    "Error creating profile: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                showLoading(false)
                            }

                    } else {
                        Log.w("LoginActivity", "Registration failed", task.exception)
                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showLoading(false)
                    }
                }
        }
    }
    private fun fetchUserRoleAndNavigate(uid: String) {
        val db = Firebase.firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    val role = user?.role ?: "Patient"
                    Log.d("LoginActivity", "User role is: $role")
                    navigateToScreen(role)
                } else {
                    Log.w("LoginActivity", "User document not found, defaulting to Patient screen")
                    navigateToScreen("Patient")
                }
            }
            .addOnFailureListener { e ->
                Log.w("LoginActivity", "Error fetching user role", e)
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    private fun navigateToScreen(role: String) {
        val intent = when (role) {
            "Patient" -> Intent(this, MainActivity::class.java)
            "Caregiver", "Doctor" -> Intent(this, DashboardActivity::class.java)
            else -> {
                Log.w("LoginActivity", "Unknown role '$role', defaulting to Patient")
                Intent(this, MainActivity::class.java)
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.groupLogin.visibility = View.GONE
            binding.groupRegister.visibility = View.GONE
            binding.btnToggleLogin.visibility = View.GONE
            binding.btnToggleRegister.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            if (binding.groupRegister.visibility == View.VISIBLE) {
                binding.btnToggleRegister.visibility = View.VISIBLE
            } else {
                binding.groupLogin.visibility = View.VISIBLE
                binding.btnToggleLogin.visibility = View.VISIBLE
            }
        }
    }
}