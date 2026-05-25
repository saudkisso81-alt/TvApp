package com.saudamertv.tvapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.core.net.toUri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.annotation.SuppressLint
import kotlin.math.abs

class MainActivity : ComponentActivity(){
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private lateinit var listView: ListView
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext10: Button
    private lateinit var btnPrev10: Button
    private lateinit var btnMoveNext: Button
    private lateinit var btnMovePrev: Button
    private lateinit var btnMoveNext10: Button
    private lateinit var btnMovePrev10: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnRecall: Button
    private lateinit var btnUndoDelete: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnFullScreen: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var etChannelNumber: EditText

    private val channelNames = mutableListOf<String>()
    private val channelUrls = mutableListOf<String>()
    private val displayNames = mutableListOf<String>()

    private var currentIndex = 0
    private var previousIndex = 0
    
    private var lastDeletedName: String? = null
    private var lastDeletedUrl: String? = null
    private var lastDeletedIndex: Int = -1
    private var isLastActionUpdate: Boolean = false
    
    private var adapter: ArrayAdapter<String>? = null
    
    private var isFullScreen = false
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // منع الشاشة من الانطفاء أو خفض الإضاءة طوال فترة فتح التطبيق
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        listView = findViewById(R.id.listChannels)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext10 = findViewById(R.id.btnNext10)
        btnPrev10 = findViewById(R.id.btnPrev10)
        btnMoveNext = findViewById(R.id.btnMoveNext)
        btnMovePrev = findViewById(R.id.btnMovePrev)
        btnMoveNext10 = findViewById(R.id.btnMoveNext10)
        btnMovePrev10 = findViewById(R.id.btnMovePrev10)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        btnRecall = findViewById(R.id.btnRecall)
        btnUndoDelete = findViewById(R.id.btnUndoDelete)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnFullScreen = findViewById(R.id.btnFullScreen)
        progressBar = findViewById(R.id.progressBar)
        etChannelNumber = findViewById(R.id.etChannelNumber)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (!isFullScreen) return false

                val diffY = e2.y - (e1?.y ?: e2.y)
                val diffX = e2.x - (e1?.x ?: e2.x)

                // Check if it's a vertical or horizontal swipe
                if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe
                    if (diffX > 100 && abs(velocityX) > 100) {
                        // Swipe Right -> Exit Full Screen
                        toggleFullScreen(false)
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (abs(diffY) > 100 && abs(velocityY) > 100) {
                        if (diffY > 0) {
                            // Swipe Down -> Next Channel
                            btnNext.performClick()
                        } else {
                            // Swipe Up -> Previous Channel
                            btnPrev.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        btnFullScreen.setOnClickListener {
            toggleFullScreen(true)
        }

        setupPlayer()

        loadM3UAndSetup()

        // Select all text when focused to make it easier to replace
        etChannelNumber.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etChannelNumber.selectAll()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            currentIndex = position
            playChannel(currentIndex)
        }

        etChannelNumber.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = etChannelNumber.text.toString()
                if (input.isNotEmpty()) {
                    val index = input.toIntOrNull()
                    if (index != null && index >= 1 && index <= channelUrls.size) {
                        currentIndex = index - 1
                        playChannel(currentIndex)
                        listView.smoothScrollToPosition(currentIndex)
                        listView.setSelection(currentIndex)
                    } else {
                        Toast.makeText(this, "Invalid Channel Number", Toast.LENGTH_SHORT).show()
                    }
                }
                hideKeyboard()
                etChannelNumber.clearFocus()
                listView.requestFocus()
                true
            } else {
                false
            }
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

        btnNext10.setOnClickListener {
            if (channelUrls.isNotEmpty()) {
                currentIndex = (currentIndex + 10).coerceAtMost(channelUrls.size - 1)
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
            }
        }

        btnPrev10.setOnClickListener {
            if (channelUrls.isNotEmpty()) {
                currentIndex = (currentIndex - 10).coerceAtMost(channelUrls.size - 1).coerceAtLeast(0)
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
            }
        }

        btnMoveNext.setOnClickListener {
            if (currentIndex < channelUrls.size - 1) {
                val nextIndex = currentIndex + 1
                
                val tempName = channelNames[currentIndex]
                channelNames[currentIndex] = channelNames[nextIndex]
                channelNames[nextIndex] = tempName
                
                val tempUrl = channelUrls[currentIndex]
                channelUrls[currentIndex] = channelUrls[nextIndex]
                channelUrls[nextIndex] = tempUrl
                
                currentIndex = nextIndex
                updateDisplayList()
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
            }
        }

        btnMovePrev.setOnClickListener {
            if (currentIndex > 0) {
                val prevIndex = currentIndex - 1
                
                val tempName = channelNames[currentIndex]
                channelNames[currentIndex] = channelNames[prevIndex]
                channelNames[prevIndex] = tempName
                
                val tempUrl = channelUrls[currentIndex]
                channelUrls[currentIndex] = channelUrls[prevIndex]
                channelUrls[prevIndex] = tempUrl
                
                currentIndex = prevIndex
                updateDisplayList()
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
            }
        }

        btnMoveNext10.setOnClickListener {
            if (channelUrls.isNotEmpty() && currentIndex < channelUrls.size - 1) {
                val targetIndex = (currentIndex + 10).coerceAtMost(channelUrls.size - 1)
                if (targetIndex != currentIndex) {
                    val name = channelNames.removeAt(currentIndex)
                    val url = channelUrls.removeAt(currentIndex)
                    channelNames.add(targetIndex, name)
                    channelUrls.add(targetIndex, url)
                    currentIndex = targetIndex
                    updateDisplayList()
                    playChannel(currentIndex)
                    listView.smoothScrollToPosition(currentIndex)
                    listView.setSelection(currentIndex)
                }
            }
        }

        btnMovePrev10.setOnClickListener {
            if (channelUrls.isNotEmpty() && currentIndex > 0) {
                val targetIndex = (currentIndex - 10).coerceAtLeast(0)
                if (targetIndex != currentIndex) {
                    val name = channelNames.removeAt(currentIndex)
                    val url = channelUrls.removeAt(currentIndex)
                    channelNames.add(targetIndex, name)
                    channelUrls.add(targetIndex, url)
                    currentIndex = targetIndex
                    updateDisplayList()
                    playChannel(currentIndex)
                    listView.smoothScrollToPosition(currentIndex)
                    listView.setSelection(currentIndex)
                }
            }
        }

        btnSave.setOnClickListener {
            saveM3U()
        }

        btnDelete.setOnClickListener {
            if (channelUrls.isNotEmpty()) {
                isLastActionUpdate = false
                val deletedName = channelNames[currentIndex]
                val deletedUrl = channelUrls[currentIndex]
                
                // Save for Undo
                lastDeletedName = deletedName
                lastDeletedUrl = deletedUrl
                lastDeletedIndex = currentIndex
                
                channelNames.removeAt(currentIndex)
                channelUrls.removeAt(currentIndex)
                
                if (channelUrls.isEmpty()) {
                    currentIndex = 0
                    previousIndex = 0
                    player.stop()
                    updateDisplayList()
                    Toast.makeText(this, "All channels deleted", Toast.LENGTH_SHORT).show()
                } else {
                    if (currentIndex >= channelUrls.size) {
                        currentIndex = channelUrls.size - 1
                    }
                    updateDisplayList()
                    playChannel(currentIndex)
                    listView.setSelection(currentIndex)
                    Toast.makeText(this, "Deleted: $deletedName", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnUndoDelete.setOnClickListener {
            if (isLastActionUpdate) {
                // التراجع عن التحديث عبر تحميل القائمة الأصلية من موقعك
                restoreOriginalListFromGithub()
            } else if (lastDeletedName != null && lastDeletedUrl != null && lastDeletedIndex != -1) {
                channelNames.add(lastDeletedIndex, lastDeletedName!!)
                channelUrls.add(lastDeletedIndex, lastDeletedUrl!!)
                
                currentIndex = lastDeletedIndex
                updateDisplayList()
                playChannel(currentIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
                
                Toast.makeText(this, "Restored: $lastDeletedName", Toast.LENGTH_SHORT).show()
                
                // Clear undo info
                lastDeletedName = null
                lastDeletedUrl = null
                lastDeletedIndex = -1
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
            }
        }

        btnRecall.setOnClickListener {
            if (channelUrls.isNotEmpty()) {
                playChannel(previousIndex)
                listView.smoothScrollToPosition(currentIndex)
                listView.setSelection(currentIndex)
            }
        }

        btnUpdate.setOnClickListener {
            updateListFromGithub()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleFullScreen(full: Boolean) {
        isFullScreen = full
        if (full) {
            findViewById<View>(R.id.row3).visibility = View.GONE
            listView.visibility = View.GONE
            findViewById<View>(R.id.row1).visibility = View.GONE
            findViewById<View>(R.id.row2).visibility = View.GONE
            
            val params = playerView.layoutParams
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams = params
            
            // Hide system UI/Status bar and navigation
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            findViewById<View>(R.id.row3).visibility = View.VISIBLE
            listView.visibility = View.VISIBLE
            findViewById<View>(R.id.row1).visibility = View.VISIBLE
            findViewById<View>(R.id.row2).visibility = View.VISIBLE
            
            val params = playerView.layoutParams
            params.height = (330 * resources.displayMetrics.density).toInt()
            playerView.layoutParams = params
            
            // Show system UI/Status bar
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        // تحسين أداء البث ومنع التقطيع عبر زيادة حجم التخزين المؤقت (Buffering)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000, // الحد الأدنى للتخزين (50 ثانية) لضمان استمرارية التشغيل عند تذبذب النت
                100000, // الحد الأقصى للتخزين (100 ثانية)
                1500,  // الوقت اللازم للبدء بعد انقطاع بسيط
                3000   // الوقت اللازم للبدء أول مرة
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // محاولة إعادة التشغيل تلقائياً عند حدوث خطأ في الشبكة بدلاً من التوقف
                Toast.makeText(this@MainActivity, "خطأ في الشبكة، جاري إعادة الاتصال...", Toast.LENGTH_SHORT).show()
                player.prepare()
                player.play()
            }
        })

        playerView.player = player

        // منع الشاشة من الانطفاء أثناء البث لضمان استمرار الصوت والصورة
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun loadM3UAndSetup() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            
            withContext(Dispatchers.IO) {
                try {
                    val file = File(filesDir, "MyList.m3u")
                    val inputStream = if (file.exists()) {
                        FileInputStream(file)
                    } else {
                        assets.open("MyList.m3u")
                    }
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    channelNames.clear()
                    channelUrls.clear()
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            progressBar.visibility = View.GONE

            if (channelNames.isNotEmpty()) {
                updateDisplayList()
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    displayNames
                )
                listView.adapter = adapter
                
                // استعادة آخر قناة كانت تعمل
                val sharedPref = getSharedPreferences("TvAppPrefs", Context.MODE_PRIVATE)
                val lastIndex = sharedPref.getInt("last_channel_index", 0)
                
                if (channelUrls.isNotEmpty()) {
                    currentIndex = if (lastIndex < channelUrls.size) lastIndex else 0
                    playChannel(currentIndex)
                    listView.setSelection(currentIndex)
                }
            } else {
                Toast.makeText(this@MainActivity, "No channels found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playChannel(index: Int) {
        if (index < 0 || index >= channelUrls.size) return
        
        if (index != currentIndex) {
            previousIndex = currentIndex
        }
        currentIndex = index
        
        // حفظ الفهرس الحالي ليتذكره التطبيق في المرة القادمة
        val sharedPref = getSharedPreferences("TvAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("last_channel_index", currentIndex)
            apply()
        }
        
        etChannelNumber.setText(String.format(Locale.US, "%04d", index + 1))

        val mediaItem = MediaItem.fromUri(
            channelUrls[index].toUri()
        )

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun updateDisplayList() {
        displayNames.clear()
        for (i in channelNames.indices) {
            displayNames.add(String.format(Locale.US, "%04d - %s", i + 1, channelNames[i]))
        }
        adapter?.notifyDataSetChanged()
    }

    private fun saveM3U() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = File(filesDir, "MyList.m3u")
                    val outputStream = FileOutputStream(file)
                    val writer = outputStream.bufferedWriter()
                    writer.write("#EXTM3U\n")

                    for (i in channelNames.indices) {
                        writer.write("#EXTINF:-1,${channelNames[i]}\n")
                        writer.write("${channelUrls[i]}\n")
                    }
                    writer.close()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            if (success) {
                Toast.makeText(this@MainActivity, "List Saved Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to Save List", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etChannelNumber.windowToken, 0)
    }

    private fun updateListFromGithub() {
        val githubUrl = "https://iptv-org.github.io/iptv/index.m3u"
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            var errorDetail = ""
            val success = withContext(Dispatchers.IO) {
                try {
                    val url = URL(githubUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val file = File(filesDir, "MyList.m3u")
                        val outputStream = FileOutputStream(file)
                        inputStream.copyTo(outputStream)
                        outputStream.close()
                        inputStream.close()
                        true
                    } else {
                        errorDetail = "Response Code: $responseCode"
                        false
                    }
                } catch (e: Exception) {
                    errorDetail = e.message ?: "Unknown error"
                    e.printStackTrace()
                    false
                }
            }
            
            progressBar.visibility = View.GONE
            
            if (success) {
                isLastActionUpdate = true
                Toast.makeText(this@MainActivity, "تم التحديث. اضغط Undo لاستعادة قائمتك الأصلية", Toast.LENGTH_LONG).show()
                loadM3UAndSetup()
            } else {
                Toast.makeText(this@MainActivity, "فشل التحديث: $errorDetail", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreOriginalListFromGithub() {
        val originalUrl = "https://raw.githubusercontent.com/saudkisso81-alt/TvApp/main/app/src/main/assets/MyList.m3u"
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val success = withContext(Dispatchers.IO) {
                try {
                    val url = URL(originalUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val file = File(filesDir, "MyList.m3u")
                        val outputStream = FileOutputStream(file)
                        inputStream.copyTo(outputStream)
                        outputStream.close()
                        inputStream.close()
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            
            progressBar.visibility = View.GONE
            
            if (success) {
                isLastActionUpdate = false
                Toast.makeText(this@MainActivity, "تم استعادة قائمتك الأصلية بنجاح!", Toast.LENGTH_SHORT).show()
                loadM3UAndSetup()
            } else {
                Toast.makeText(this@MainActivity, "فشل في استعادة القائمة الأصلية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}