package vcmsa.projects.tycoonpoe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class Home : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlayGame = view.findViewById<Button>(R.id.btnPlayGame)
        val btnProfile = view.findViewById<Button>(R.id.btnProfile)
        val btnLogout = view.findViewById<Button>(R.id.btnSettings) // your Logout button

        // Navigate to Play Game screen
        btnPlayGame.setOnClickListener {
            findNavController().navigate(R.id.action_home2_to_gameModes) // replace with your actual action ID
        }

        // Navigate to Profile screen
        btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home2_to_profile2) // replace with your actual action ID
        }

        // Logout and go back to Login
        btnLogout.setOnClickListener {
            // Optional: clear any saved session info here
            findNavController().navigate(R.id.action_home2_to_login)
        }
    }
}
