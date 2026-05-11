package com.saudamertv.tvapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.net.toUri

class MainActivity : ComponentActivity(){
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private lateinit var listView: ListView
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var progressBar: ProgressBar

    private val channelNames = mutableListOf<String>()
    private val channelUrls = mutableListOf<String>()

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        listView = findViewById(R.id.listChannels)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        progressBar = findViewById(R.id.progressBar)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        loadM3UAndSetup()

        listView.setOnItemClickListener { _, _, position, _ ->
            currentIndex = position
            playChannel(currentIndex)
        }

        btnNext.setOnClickListener {
            if (currentIndex < channelUrls.size - 1) {
                currentIndex++
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
            }
        }

        // Set focus to the list so D-pad works immediately
        listView.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                btnNext.performClick()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                btnPrev.performClick()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // If the list is focused, the item click will handle it.
                // Otherwise, we might want to toggle play/pause or show UI.
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun loadM3UAndSetup() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            
            withContext(Dispatchers.IO) {
                val inputStream = assets.open("MyList.m3u")
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("#EXTINF") == true) {
                        val name = line.substringAfter(",")
                        val url = reader.readLine()
                        if (url != null) {
                            channelNames.add(name)
                            channelUrls.add(url)
                        }
                    }
                }
                reader.close()
            }

            progressBar.visibility = View.GONE

            if (channelNames.isNotEmpty()) {
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    channelNames
                )
                listView.adapter = adapter
                
                // Autoplay first channel
                if (channelUrls.isNotEmpty()) {
                    playChannel(2)
                }
            } else {
                Toast.makeText(this@MainActivity, "No channels found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playChannel(index: Int) {
        if (index < 0 || index >= channelUrls.size) return

        val mediaItem = MediaItem.fromUri(
            channelUrls[index].toUri()
        )

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}