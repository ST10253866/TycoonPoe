package vcmsa.projects.tycoonpoe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class GameModes : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game_modes, container, false)

        // Find buttons
        val btnOffline = view.findViewById<ImageButton>(R.id.btnOffline)
        val btnOnline = view.findViewById<ImageButton>(R.id.btnOnline)

        // Offline button launches full-screen OfflineGameActivity
        btnOffline.setOnClickListener {
            val intent = Intent(requireContext(), OfflineGameActivity::class.java)
            startActivity(intent)
        }

        val backButton = view.findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_gameModes_to_home2)
        }


        // Online button launches full-screen OnlineGameActivity
        btnOnline.setOnClickListener {
            val intent = Intent(requireContext(), OnlineGameActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
