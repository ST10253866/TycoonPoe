package vcmsa.projects.tycoonpoe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : Fragment() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var googleSignInClient: GoogleSignInClient

    private val settingsDao by lazy { TycoonDatabase.getDatabase(requireContext()).settingsDao() }
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val RC_GOOGLE_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Biometric setup ---
        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(requireContext(), "Biometric authentication succeeded!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_login_to_home2)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Biometric authentication failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(requireContext(), "Biometric error: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Authenticate with your fingerprint")
            .setNegativeButtonText("Cancel")
            .build()

        // --- Google Sign-In setup ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // make sure this exists
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameInput = view.findViewById<EditText>(R.id.etUsername)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val btnAuthenticate = view.findViewById<Button>(R.id.btnAuthenticate)
        val btnGoogleSignIn = view.findViewById<LinearLayout>(R.id.btnGoogleSignIn) // <-- custom LinearLayout button
        val tvGoToRegister = view.findViewById<TextView>(R.id.tvGoToRegister)

        // --- Navigate to Register ---
        tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        // --- Email/Password login ---
        btnAuthenticate.setOnClickListener {
            val email = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleBiometricOrHome()
                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- Google Sign-In login ---
        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }

    private fun handleBiometricOrHome() {
        lifecycleScope.launch {
            val settings = withContext(Dispatchers.IO) { settingsDao.getSettings() }
            val biometricsDisabled = settings?.biometricsDisabled ?: false

            if (!biometricsDisabled) {
                val biometricManager = BiometricManager.from(requireContext())
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Toast.makeText(requireContext(), "No biometric hardware", Toast.LENGTH_SHORT).show()
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Toast.makeText(requireContext(), "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Toast.makeText(requireContext(), "No fingerprint enrolled", Toast.LENGTH_SHORT).show()
                }
            } else {
                findNavController().navigate(R.id.action_login_to_home2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        handleBiometricOrHome()
                    } else {
                        Toast.makeText(requireContext(), "Google Sign-In failed: ${authTask.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
