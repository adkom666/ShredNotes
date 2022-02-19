package com.adkom666.shrednotes.sound

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.adkom666.shrednotes.R
import timber.log.Timber

/**
 * Way to play sounds in the application.
 *
 * @param context [Context].
 */
class ShredSound(context: Context) {

    /**
     * Sound name.
     */
    enum class Track {
        JOY,
        SAD
    }

    private val playerMap: Map<Track, MediaPlayer>
    private var currentPlayer: MediaPlayer? = null

    init {
        val playerMap = mutableMapOf<Track, MediaPlayer>()
        Track.values().forEach { track ->
            playerMap[track] = MediaPlayer.create(context, track.resId())
        }
        this.playerMap = playerMap
    }

    /**
     * Play sound.
     *
     * @param track name of sound to play.
     */
    fun play(track: Track) {
        Timber.d("Play: track=$track")
        currentPlayer?.pause()
        currentPlayer = playerMap[track]?.let { player ->
            player.seekTo(0)
            player.start()
            player
        }
    }

    @RawRes
    private fun Track.resId(): Int = when (this) {
        Track.JOY -> R.raw.joy
        Track.SAD -> R.raw.sad
    }
}
