package com.app.echomi

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView // Changed from ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.echomi.Adapter.TranscriptAdapter
import com.app.echomi.Network.RetrofitInstance
import com.app.echomi.data.CallLog
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class CallDetailScreen : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var transcriptAdapter: TranscriptAdapter
    private lateinit var transcriptRecyclerView: RecyclerView

    // Track player state manually since we are using a custom logic
    private var isPlayerPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call_detail_screen)

        val callId = intent.getStringExtra("CALL_ID")
        if (callId == null) {
            Toast.makeText(this, "Error: Call ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupTranscriptRecyclerView()
        fetchCallDetails(callId)
    }

    private fun setupTranscriptRecyclerView() {
        transcriptRecyclerView = findViewById(R.id.transcriptRecyclerView)
        transcriptAdapter = TranscriptAdapter()
        transcriptRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CallDetailScreen)
            adapter = transcriptAdapter
        }
    }

    private fun fetchCallDetails(callId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCallLogById(callId)
                if (response.isSuccessful) {
                    val log = response.body()
                    if (log != null) {
                        bindDataToUi(log)
                    } else {
                        throw Exception("Call log data is null")
                    }
                } else {
                    throw Exception("Failed to load details")
                }
            } catch (e: Exception) {
                Toast.makeText(this@CallDetailScreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindDataToUi(log: CallLog) {
        findViewById<TextView>(R.id.callerNumberTextView).text = log.callerNumber
        findViewById<TextView>(R.id.summaryTextView).text = log.summary ?: "No summary provided."

        // Format the date
        val formattedDate = try {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(log.startTime)
            java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            log.startTime.take(10) // fallback
        }
        findViewById<TextView>(R.id.dateTimeTextView).text = formattedDate

        // Setup the recording player if a URL exists
        if (!log.recordingUrl.isNullOrEmpty()) {
            setupMediaPlayer(log.recordingUrl)
        }

        // Display transcript
        if (log.transcript.isNotEmpty()) {
            transcriptAdapter.updateTranscript(log.transcript)
            // Note: Header text is hardcoded as "TRANSCRIPT LOG" in new XML,
            // but you can update it dynamically if you wish:
            // findViewById<TextView>(R.id.transcriptHeaderTextView)?.text = "TRANSCRIPT LOG (${log.transcript.size})"
        }
    }

    private fun setupMediaPlayer(url: String) {
        val playerLayout: LinearLayout = findViewById(R.id.recordingPlayerLayout)

        // FIX: Cast to ImageView instead of ImageButton
        val playPauseButton: ImageView = findViewById(R.id.playPauseButton)
        val seekBar: SeekBar = findViewById(R.id.seekBar)

        playerLayout.visibility = View.VISIBLE

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { mp ->
                    isPlayerPrepared = true
                    seekBar.max = mp.duration
                }
                setOnCompletionListener {
                    playPauseButton.setImageResource(R.drawable.play)
                    seekBar.progress = 0
                }
            } catch (e: Exception) {
                Toast.makeText(this@CallDetailScreen, "Audio Error", Toast.LENGTH_SHORT).show()
            }
        }

        playPauseButton.setOnClickListener {
            if (!isPlayerPrepared || mediaPlayer == null) return@setOnClickListener

            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                playPauseButton.setImageResource(R.drawable.play) // Ensure you have ic_play or play drawable
            } else {
                mediaPlayer!!.start()
                playPauseButton.setImageResource(R.drawable.pause) // Ensure you have ic_pause or pause drawable
                updateSeekBar()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isPlayerPrepared) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun updateSeekBar() {
        if (mediaPlayer?.isPlaying == true) {
            findViewById<SeekBar>(R.id.seekBar).progress = mediaPlayer!!.currentPosition
            handler.postDelayed({ updateSeekBar() }, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}