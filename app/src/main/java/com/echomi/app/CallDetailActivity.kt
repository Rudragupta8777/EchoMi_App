package com.echomi.app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.echomi.app.data.CallLog
import com.echomi.app.network.RetrofitInstance
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.text.isNullOrEmpty

class CallDetailActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_detail)

        val callId = intent.getStringExtra("CALL_ID")
        if (callId == null) {
            Toast.makeText(this, "Error: Call ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        fetchCallDetails(callId)
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
                Toast.makeText(this@CallDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindDataToUi(log: CallLog) {
        findViewById<TextView>(R.id.callerNumberTextView).text = log.callerNumber
        findViewById<TextView>(R.id.summaryTextView).text = log.summary ?: "No summary provided."
        findViewById<TextView>(R.id.dateTimeTextView).text = log.startTime // TODO: Format this date

        // Setup the recording player if a URL exists
        if (!log.recordingUrl.isNullOrEmpty()) {
            setupMediaPlayer(log.recordingUrl)
        }

        // TODO: Setup a RecyclerView and Adapter to display the transcript
    }

    private fun setupMediaPlayer(url: String) {
        val playerLayout: LinearLayout = findViewById(R.id.recordingPlayerLayout)
        val playPauseButton: ImageButton = findViewById(R.id.playPauseButton)
        val seekBar: SeekBar = findViewById(R.id.seekBar)
        playerLayout.visibility = View.VISIBLE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { mp ->
                seekBar.max = mp.duration
                playPauseButton.setOnClickListener {
                    if (isPlaying) {
                        pause()
                        playPauseButton.setImageResource(R.drawable.play) // Change to your play icon
                    } else {
                        start()
                        playPauseButton.setImageResource(R.drawable.pause) // Change to your pause icon
                        updateSeekBar()
                    }
                }
            }
            setOnCompletionListener {
                playPauseButton.setImageResource(R.drawable.play)
                seekBar.progress = 0
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
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
    }
}