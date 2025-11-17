package vcmsa.projects.tycoonpoe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Profile : Fragment() {

    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnGoToSettings: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        editUsername = view.findViewById(R.id.editUsername)
        editPassword = view.findViewById(R.id.editPassword)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)
        btnGoToSettings = view.findViewById(R.id.btnGoToSettings)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_profile2_to_home2)
        }

        // Load current user info
        val currentUser = auth.currentUser
        currentUser?.let {
            editUsername.setText(it.email) // assuming username = email
        }

        // Update profile info
        btnSaveProfile.setOnClickListener {
            val newEmail = editUsername.text.toString().trim()
            val newPassword = editPassword.text.toString().trim()

            if (newEmail.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                // Update email
                user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                    if (emailTask.isSuccessful) {
                        // Update password
                        user.updatePassword(newPassword).addOnCompleteListener { passTask ->
                            if (passTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()

                                // ================================
                                // SEND NOTIFICATION AFTER UPDATE
                                // ================================
                                (activity as? MainActivity)?.sendInstantNotification(
                                    "Profile Updated",
                                    "Your email and password have been successfully updated."
                                )

                            } else {
                                Toast.makeText(requireContext(), "Password update failed: ${passTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Email update failed: ${emailTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Navigate to Settings
        btnGoToSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profile2_to_settings)
        }
    }
}
