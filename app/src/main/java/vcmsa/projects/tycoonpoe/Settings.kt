package vcmsa.projects.tycoonpoe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.util.*

class Settings : Fragment() {

    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnDisableBiometrics: Button
    private lateinit var cardBack1: ImageButton
    private lateinit var cardBack2: ImageButton
    private lateinit var cardBack3: ImageButton

    private var selectedCardBack = "card_back1"
    private var biometricsDisabled = false
    private var currentLanguageCode = "en"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        btnDisableBiometrics = view.findViewById(R.id.btnDisableBiometrics)
        cardBack1 = view.findViewById(R.id.cardBack1)
        cardBack2 = view.findViewById(R.id.cardBack2)
        cardBack3 = view.findViewById(R.id.cardBack3)

        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_profile2)
        }

        // Setup language spinner
        val languages = arrayOf("English", "Afrikaans", "Zulu")
        spinnerLanguage.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)

        // Load saved settings
        val dao = TycoonDatabase.getDatabase(requireContext()).settingsDao()
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = dao.getSettings()
            saved?.let {
                // Card back & biometrics
                selectedCardBack = it.selectedCardBack
                biometricsDisabled = it.biometricsDisabled
                btnDisableBiometrics.text =
                    if (biometricsDisabled) "Enable Biometrics" else "Disable Biometrics"
                highlightSelectedCard()

                // Language
                val langPosition = languages.indexOf(it.language)
                if (langPosition != -1) spinnerLanguage.setSelection(langPosition)
                currentLanguageCode = getLanguageCode(it.language)
                setAppLocale(currentLanguageCode)
            }
        }

        // Card back selection
        cardBack1.setOnClickListener { selectCardBack("card_back1") }
        cardBack2.setOnClickListener { selectCardBack("card_back2") }
        cardBack3.setOnClickListener { selectCardBack("card_back3") }

        // Language change logic
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLangCode = when (position) {
                    0 -> "en"
                    1 -> "af"
                    2 -> "zu"
                    else -> "en"
                }
                if (selectedLangCode != currentLanguageCode) {
                    currentLanguageCode = selectedLangCode
                    setAppLocale(currentLanguageCode)
                    saveSettings() // save language change
                    // reload fragment to apply language
                    findNavController().navigate(findNavController().currentDestination!!.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Biometrics toggle
        btnDisableBiometrics.setOnClickListener {
            biometricsDisabled = !biometricsDisabled
            btnDisableBiometrics.text =
                if (biometricsDisabled) "Enable Biometrics" else "Disable Biometrics"
            saveSettings()
        }

        return view
    }

    private fun selectCardBack(cardBack: String) {
        selectedCardBack = cardBack
        highlightSelectedCard()
        saveSettings() // save card back without reloading fragment
    }

    private fun highlightSelectedCard() {
        cardBack1.setBackgroundResource(android.R.color.transparent)
        cardBack2.setBackgroundResource(android.R.color.transparent)
        cardBack3.setBackgroundResource(android.R.color.transparent)

        when (selectedCardBack) {
            "card_back1" -> cardBack1.setBackgroundResource(R.drawable.card_selected_border)
            "card_back2" -> cardBack2.setBackgroundResource(R.drawable.card_selected_border)
            "card_back3" -> cardBack3.setBackgroundResource(R.drawable.card_selected_border)
        }
    }

    private fun saveSettings() {
        val dao = TycoonDatabase.getDatabase(requireContext()).settingsDao()
        val selectedLanguage = spinnerLanguage.selectedItem.toString()

        viewLifecycleOwner.lifecycleScope.launch {
            val settings = SettingsEntity(
                id = 1,
                language = selectedLanguage,
                selectedCardBack = selectedCardBack,
                biometricsDisabled = biometricsDisabled
            )
            dao.saveSettings(settings)

            // Send notification
            (activity as? MainActivity)?.sendInstantNotification(
                "Settings Saved",
                "Your app settings have been successfully updated."
            )
        }
    }

    // Language code helper
    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en"
            "Afrikaans" -> "af"
            "Zulu" -> "zu"
            else -> "en"
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        requireActivity().resources.updateConfiguration(config, requireActivity().resources.displayMetrics)
    }
}
