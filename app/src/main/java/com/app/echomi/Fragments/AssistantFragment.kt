package com.app.echomi.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.app.echomi.R

class AssistantFragment : Fragment() {

    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the new "Obsidian & Emerald" dashboard layout
        val view = inflater.inflate(R.layout.fragment_assistant, container, false)

        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)

        setupVisualizer()

        return view
    }

    private fun setupVisualizer() {
        // Configure the "Brain" visualizer
        try {
            lottieAnimationView.speed = 1.0f
            // If the XML has app:lottie_autoPlay="true", this is technically redundant
            // but good for safety if the view is recycled.
            lottieAnimationView.playAnimation()
        } catch (e: Exception) {
            lottieAnimationView.visibility = View.GONE
        }

        // Handle animation failures gracefully
        lottieAnimationView.addLottieOnCompositionLoadedListener { composition ->
            if (composition == null) {
                lottieAnimationView.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save resources when the user switches tabs
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume the "System Online" visualizer
        if (::lottieAnimationView.isInitialized && lottieAnimationView.visibility == View.VISIBLE) {
            try {
                lottieAnimationView.playAnimation()
            } catch (e: Exception) {
                lottieAnimationView.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.cancelAnimation()
        }
    }
}