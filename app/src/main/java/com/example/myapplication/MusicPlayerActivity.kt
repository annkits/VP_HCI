package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import java.util.Locale

class MusicPlayerActivity : AppCompatActivity() {
    private lateinit var bBackToMain: Button
    private lateinit var songListView: ListView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeTextView: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var currentSongTextView: TextView
    private lateinit var mediaPlayer: MediaPlayer
    private val songList = mutableListOf<String>()
    private val songPaths = mutableListOf<String>()
    private var currentSongIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadSongsFromStorage()
            Toast.makeText(this, "Можно читать музыку", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Без разрешения музыка недоступна", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_music_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupMediaPlayer()
        setupClickListeners()
        checkAndRequestPermission()
    }

    private fun initViews() {
        songListView = findViewById(R.id.songListView)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeTextView = findViewById(R.id.totalTimeText)
        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        currentSongTextView = findViewById(R.id.currentSongText)
        bBackToMain = findViewById(R.id.back_to_main)
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { nextSong() }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = mediaPlayer.duration
                    val newPos = (progress * duration) / 100
                    mediaPlayer.seekTo(newPos)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) pauseMusic() else playMusic()
        }
        nextButton.setOnClickListener { nextSong() }
        previousButton.setOnClickListener { previousSong() }
        bBackToMain.setOnClickListener{
            val backToMain = Intent(this, MainActivity::class.java)
            startActivity(backToMain)
        }
        songListView.setOnItemClickListener { _, _, position, _ ->
            currentSongIndex = position
            playSelectedSong(position)
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        audioPermissionLauncher.launch(permission)
    }

    private fun loadSongsFromStorage() {
        songList.clear()
        songPaths.clear()

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        if (!musicDir.exists() || !musicDir.isDirectory) {
            Toast.makeText(this, "Папка Music не найдена", Toast.LENGTH_LONG).show()
            return
        }

        val files = musicDir.listFiles() ?: return

        for (file in files) {
            if (file.isFile) {
                val name = file.name.lowercase()
                if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a")) {
                    songList.add(file.name)
                    songPaths.add(file.absolutePath)
                }
            }
        }

        if (songList.isEmpty()) {
            Toast.makeText(this, "Музыка не найдена", Toast.LENGTH_LONG).show()
            return
        }

        songListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songList)
        playSelectedSong(0)
    }

    private fun playSelectedSong(index: Int) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, "file://${songPaths[index]}".toUri())
            mediaPlayer.prepare()
            mediaPlayer.start()

            currentSongIndex = index
            currentSongTextView.text = songList[index]
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)

            totalTimeTextView.text = formatTime(mediaPlayer.duration)
            updateSeekBar()
    }

    private fun playMusic() {
        mediaPlayer.start()
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        updateSeekBar()
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        handler.removeCallbacksAndMessages(null)
    }

    private fun nextSong() {
        if (songList.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songList.size
            playSelectedSong(currentSongIndex)
        }
    }

    private fun previousSong() {
        if (songList.isNotEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + songList.size) % songList.size
            playSelectedSong(currentSongIndex)
        }
    }

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer.let { mp ->
                    if (mp.isPlaying) {
                        val current = mp.currentPosition
                        val total = mp.duration
                        val progress = if (total > 0) (current * 100 / total) else 0

                        seekBar.progress = progress
                        currentTimeText.text = formatTime(current)
                        totalTimeTextView.text = formatTime(total)

                        handler.postDelayed(this, 1000)
                    }
                }
            }
        })
    }

    private fun formatTime(millis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacksAndMessages(null)
    }
}
