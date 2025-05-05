package com.sozdle.sozdle.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import com.sozdle.sozdle.R

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop any existing playback
        stopMusic()

        // Get AudioManager to control system volume
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Request audio focus
        val result = audioManager.requestAudioFocus(
            AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> stopMusic()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mediaPlayer?.pause()
                    AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.start()
                }
            },
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("MusicService", "Audio focus not granted")
            return START_NOT_STICKY
        }

        // Save current volume and set to maximum
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        Log.d("MusicService", "Set media volume to max: $maxVolume")

        // Initialize and start MediaPlayer with the music file
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.game_music)
            mediaPlayer?.apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                isLooping = true // Loop the music
                setVolume(1.0f, 1.0f) // Set maximum MediaPlayer volume
                start()
            }
            Log.d("MusicService", "Music started with max system and player volume")
        } catch (e: Exception) {
            Log.e("MusicService", "Error starting music", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
        Log.d("MusicService", "MusicService destroyed")
    }

    private fun stopMusic() {
        // Abandon audio focus
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)

        // Restore original volume if available
        originalVolume?.let { volume ->
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            Log.d("MusicService", "Restored media volume to: $volume")
        }

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}