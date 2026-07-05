package com.example.simplevttplayer // **<<< CHECK THIS LINE CAREFULLY!**

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStream
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider // Import Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import com.google.android.material.switchmaterial.SwitchMaterial
import android.view.View
import android.view.WindowManager // *** Import for Keep Screen On ***
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

// Settings accessors (extension properties on Context)
import com.example.simplevttplayer.AppSettings.captionTextSizeSp
import com.example.simplevttplayer.AppSettings.captionTextColor
import com.example.simplevttplayer.AppSettings.captionBoxColor
import com.example.simplevttplayer.AppSettings.playbackSpeed
import com.example.simplevttplayer.AppSettings.keepScreenOn
import com.example.simplevttplayer.AppSettings.preRollMs
import com.example.simplevttplayer.AppSettings.postRollMs
import com.example.simplevttplayer.AppSettings.fullscreenBgColor
import com.example.simplevttplayer.AppSettings.timeFormatIndex

class MainActivity : AppCompatActivity() {

    // --- Constants ---
    companion object {
        private const val ACTION_UPDATE_SUBTITLE_LOCAL = OverlayService.ACTION_UPDATE_SUBTITLE
        private const val EXTRA_SUBTITLE_TEXT_LOCAL = OverlayService.EXTRA_SUBTITLE_TEXT
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    // --- UI Elements ---
    private lateinit var buttonSelectFile: MaterialButton
    private lateinit var buttonSettings: MaterialButton
    private lateinit var buttonFullscreen: MaterialButton
    private lateinit var textViewFilePath: TextView
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewSubtitle: TextView
    private lateinit var buttonPlayPause: MaterialButton
    private lateinit var buttonReset: MaterialButton
    private lateinit var buttonLaunchOverlay: MaterialButton
    private lateinit var sliderPlayback: Slider

    // Skip controls + seek buttons
    private lateinit var buttonPrevCue: MaterialButton
    private lateinit var buttonNextCue: MaterialButton
    private val seekButtons = mutableListOf<MaterialButton>()

    // Fullscreen UI
    private lateinit var fullscreenContainer: View
    private lateinit var textViewFullscreenSubtitle: TextView
    private lateinit var fullscreenControls: View
    private lateinit var buttonFsPlayPause: MaterialButton
    private lateinit var buttonFsPrev: MaterialButton
    private lateinit var buttonFsNext: MaterialButton
    private lateinit var buttonFsExit: MaterialButton

    // --- Subtitle Data ---
    data class SubtitleCue( val startTimeMs: Long, val endTimeMs: Long, val text: String )
    private var subtitleCues: List<SubtitleCue> = emptyList()
    private var selectedFileUri: Uri? = null

    // --- Playback & UI State ---
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var startTimeNanos: Long = 0L
    private var pausedElapsedTimeMillis: Long = 0L
    private var currentCueIndex: Int = -1
    private var wasPlayingBeforeSeek = false
    private var isOverlayUIShown = true
    private var isFullscreen = false

    // --- File Selection Launcher ---
    private val selectSubtitleFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri -> loadSubtitleFromUri(uri) }
        } else {
            Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Activity Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Uses layout with Material components
        setSupportActionBar(findViewById(R.id.toolbar))

        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (checkOverlayPermission()) { Log.d(TAG, "Overlay perm granted post-settings."); startOverlayService()
            } else { Log.w(TAG, "Overlay perm not granted post-settings."); Toast.makeText(this, "Overlay permission required.", Toast.LENGTH_SHORT).show() }
        }

        // Init UI Elements
        buttonSelectFile = findViewById(R.id.buttonSelectFile)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonFullscreen = findViewById(R.id.buttonFullscreen)
        textViewFilePath = findViewById(R.id.textViewFilePath)
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime)
        textViewSubtitle = findViewById(R.id.textViewSubtitle)
        buttonPlayPause = findViewById(R.id.buttonPlayPause)
        buttonReset = findViewById(R.id.buttonReset)
        buttonLaunchOverlay = findViewById(R.id.buttonLaunchOverlay)
        sliderPlayback = findViewById(R.id.sliderPlayback) // Use Slider ID

        buttonPrevCue = findViewById(R.id.buttonPrevCue)
        buttonNextCue = findViewById(R.id.buttonNextCue)

        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        textViewFullscreenSubtitle = findViewById(R.id.textViewFullscreenSubtitle)
        fullscreenControls = findViewById(R.id.fullscreenControls)
        buttonFsPlayPause = findViewById(R.id.buttonFsPlayPause)
        buttonFsPrev = findViewById(R.id.buttonFsPrev)
        buttonFsNext = findViewById(R.id.buttonFsNext)
        buttonFsExit = findViewById(R.id.buttonFsExit)

        // Set Listeners
        buttonSelectFile.setOnClickListener { openFilePicker() }
        buttonSettings.setOnClickListener { showSettingsSheet() }
        buttonFullscreen.setOnClickListener { enterFullscreen() }
        buttonPlayPause.setOnClickListener { togglePlayPause() }
        buttonReset.setOnClickListener { resetPlayback() }
        buttonPrevCue.setOnClickListener { skipToCue(-1) }
        buttonNextCue.setOnClickListener { skipToCue(+1) }
        buttonFsPrev.setOnClickListener { skipToCue(-1) }
        buttonFsNext.setOnClickListener { skipToCue(+1) }
        listOf(
            R.id.buttonSeekM5s  to -5000L,
            R.id.buttonSeekM07s to  -700L,
            R.id.buttonSeekM01s to  -100L,
            R.id.buttonSeekP01s to   100L,
            R.id.buttonSeekP07s to   700L,
            R.id.buttonSeekP5s  to  5000L
        ).forEach { (id, delta) ->
            val btn = findViewById<MaterialButton>(id)
            btn.setOnClickListener { seekBy(delta) }
            seekButtons.add(btn)
        }
        buttonLaunchOverlay.setOnClickListener {
            isOverlayUIShown = !isOverlayUIShown
            if (isOverlayUIShown) { Log.d(TAG,"Overlay ON"); Toast.makeText(this,"Overlay Shown",Toast.LENGTH_SHORT).show(); handleLaunchOverlayClick(); sendStyleUpdate(); sendSubtitleUpdate(textViewSubtitle.text.toString())
            } else { Log.d(TAG,"Overlay OFF"); Toast.makeText(this,"Overlay Hidden",Toast.LENGTH_SHORT).show(); sendSubtitleUpdate("") }
        }
        setupSliderListener() // Setup listener for the Slider

        // Fullscreen listeners
        fullscreenContainer.setOnClickListener { toggleFullscreenControls() }
        buttonFsPlayPause.setOnClickListener { togglePlayPause() }
        buttonFsExit.setOnClickListener { exitFullscreen() }

        // Back button: leave fullscreen first if active
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    exitFullscreen()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Initial state
        buttonLaunchOverlay.isEnabled = false
        sliderPlayback.isEnabled = false
        setPlayButtonState(false) // Ensure correct initial icon
        applyCaptionStyle()       // Apply saved caption look

        // Handle file opened from another app
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { loadSubtitleFromUri(it) }
        }
    }

    // *** ADDED Keep Screen On flag clearing ***
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        stopOverlayService()
        // Ensure screen on flag is cleared if activity is destroyed
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "onDestroy: Stopped service & cleared keep screen on flag.")
    }

    // --- Slider Setup ---
    private fun setupSliderListener() {
        sliderPlayback.setLabelFormatter { value -> formatTime(value.toLong()) }
        sliderPlayback.addOnChangeListener { _, value, fromUser ->
            if (fromUser) { textViewCurrentTime.text = formatTime(value.toLong()) }
        }
        sliderPlayback.addOnSliderTouchListener(object : OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
                wasPlayingBeforeSeek = isPlaying
                if (isPlaying) { pausePlayback() }
                Log.d(TAG, "Slider touch started.")
            }
            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                val seekToMillis = slider.value.toLong()
                Log.d(TAG, "Seek finished via Slider at: $seekToMillis ms")
                pausedElapsedTimeMillis = seekToMillis
                textViewCurrentTime.text = formatTime(pausedElapsedTimeMillis)
                val currentCue = findCueForTime(pausedElapsedTimeMillis)
                val currentText = currentCue?.text ?: ""
                setCaptionText(currentText)
                sendSubtitleUpdate(currentText)
                rebaseClock()
                if (wasPlayingBeforeSeek) { startPlayback() }
                else { setPlayButtonState(false) } // Ensure icon is Play if not resuming
            }
        })
    }

    // --- File Handling & Parsing ---
    @SuppressLint("Range") private fun getFileName(uri: Uri): String? { var f: String? = null; try { contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) { val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i != -1) f = c.getString(i) } } } catch (e: Exception) { Log.e(TAG, "getFileName error: $uri", e) }; if (f == null) { f = uri.path; val cut = f?.lastIndexOf('/'); if (cut != -1 && cut != null) { f = f?.substring(cut + 1) } }; return f }
    private fun openFilePicker() { val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }; selectSubtitleFileLauncher.launch(i) }

    private fun loadSubtitleFromUri(uri: Uri) {
        selectedFileUri = uri
        val fileName = getFileName(uri)
        resetPlayback()
        if (fileName != null) {
            textViewFilePath.text = "File: $fileName"
            when {
                fileName.lowercase().endsWith(".vtt") -> loadAndParseSubtitleFile(uri, "vtt")
                fileName.lowercase().endsWith(".srt") -> loadAndParseSubtitleFile(uri, "srt")
                else -> {
                    Toast.makeText(this, "Not VTT/SRT ($fileName)", Toast.LENGTH_LONG).show()
                    resetPlaybackStateOnError()
                    textViewFilePath.text = "File: $fileName (Not VTT/SRT?)"
                }
            }
        } else {
            Toast.makeText(this, "No filename.", Toast.LENGTH_SHORT).show()
            resetPlaybackStateOnError()
            textViewFilePath.text = "File: (Unknown)"
        }
    }

    private fun loadAndParseSubtitleFile(uri: Uri, format: String) {
        Log.d(TAG, "Attempting to load $format file: $uri")
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                subtitleCues = if (format == "vtt") parseVtt(inputStream) else parseSrt(inputStream)
                if (subtitleCues.isNotEmpty()) {
                    Toast.makeText(this, "${format.uppercase()} loaded: ${subtitleCues.size} cues", Toast.LENGTH_SHORT).show()
                    buttonPlayPause.isEnabled = true; buttonReset.isEnabled = true; buttonLaunchOverlay.isEnabled = true
                    buttonPrevCue.isEnabled = true; buttonNextCue.isEnabled = true
                    seekButtons.forEach { it.isEnabled = true }
                    val duration = subtitleCues.lastOrNull()?.endTimeMs ?: 0L
                    sliderPlayback.valueFrom = 0.0f; sliderPlayback.valueTo = duration.toFloat(); sliderPlayback.value = 0.0f; sliderPlayback.isEnabled = true
                    setCaptionText("[Ready to play]"); textViewCurrentTime.text = formatTime(0)
                    isOverlayUIShown = true; setPlayButtonState(false); sendSubtitleUpdate("")
                } else { Toast.makeText(this, "No cues parsed.", Toast.LENGTH_LONG).show(); resetPlaybackStateOnError() }
            } ?: run { Toast.makeText(this, "Failed file stream.", Toast.LENGTH_LONG).show(); Log.w(TAG, "Null InputStream: $uri"); resetPlaybackStateOnError() }
        } catch (e: Exception) { Log.e(TAG, "load/parse $format error", e); Toast.makeText(this, "Load ${format.uppercase()} error: ${e.message}", Toast.LENGTH_LONG).show(); resetPlaybackStateOnError() }
    }

    // --- VTT Parsing Logic ---
    private fun parseVtt(inputStream: InputStream): List<SubtitleCue> { val c=mutableListOf<SubtitleCue>(); val r=inputStream.bufferedReader(Charsets.UTF_8); try { var h=r.readLine(); if(h?.startsWith("\uFEFF")==true){h=h.substring(1)}; if(h==null||!h.trim().startsWith("WEBVTT")){Log.e("VTTParser","Bad Header: '$h'"); runOnUiThread { Toast.makeText(this,"Bad VTT Header",Toast.LENGTH_LONG).show() }; return emptyList()}; var l:String?; while(r.readLine().also{l=it}!=null){ val t=l?.trim()?:""; if(t.isEmpty()||t.startsWith("NOTE")){continue}; if(t.contains("-->")){parseTimeAndTextVtt(t,r,c)}else{Log.w("VTTParser","Skip line: $t")}} }catch(e:Exception){Log.e("VTTParser","Parse VTT Error",e); runOnUiThread { Toast.makeText(this,"VTT Process Error",Toast.LENGTH_SHORT).show() }}finally{try{r.close()}catch(e:Exception){}}; return c.sortedBy{it.startTimeMs}}
    private fun parseTimeAndTextVtt(tL: String, r: BufferedReader, c: MutableList<SubtitleCue>) { try { val t=tL.split("-->"); if(t.size<2){Log.w("VTTParser","Bad time: $tL"); return}; val s=timeToMillis(t[0].trim()); val eS=t[1].trim().split(Regex("\\s+"))[0]; val e=timeToMillis(eS); val b=StringBuilder(); var x:String?=r.readLine(); while(x!=null&&x.isNotBlank()){if(b.isNotEmpty())b.append("\n"); b.append(x); x=r.readLine()}; if(s!=null&&e!=null&&b.isNotEmpty()){if(e>s){c.add(SubtitleCue(s,e,b.toString()))}else{Log.w("VTTParser","End<=Start: $tL")}}else{Log.w("VTTParser","Bad cue: $tL")}}catch(e:Exception){Log.e("VTTParser","Parse cue error: $tL", e)}}

    // --- SRT Parsing Logic ---
    private fun parseSrt(inputStream: InputStream): List<SubtitleCue> { val c=mutableListOf<SubtitleCue>(); val r=inputStream.bufferedReader(Charsets.UTF_8); try { var l: String?; while (r.readLine().also { l = it } != null) { val tL = l?.trim(); if (tL.isNullOrEmpty()) continue; if (tL.toIntOrNull() != null) { val timeL = r.readLine()?.trim(); if (timeL != null && timeL.contains("-->")) { val ts = timeL.split("-->"); if (ts.size >= 2) { val s = timeToMillis(ts[0].trim().replace(',', '.')); val eS = ts[1].trim().split(Regex("\\s+"))[0]; val e = timeToMillis(eS.replace(',', '.')); val b = StringBuilder(); var txtL: String? = r.readLine(); while (txtL != null && !txtL.isBlank()) { if (b.isNotEmpty()) b.append("\n"); b.append(txtL); txtL = r.readLine() }; if (txtL == null && b.isEmpty() && ts[0].trim().isNotEmpty()) { Log.w("SRTParser", "Malformed SRT end: $timeL") }; if (s != null && e != null && b.isNotEmpty() && e > s) { c.add(SubtitleCue(s, e, b.toString())) } else { Log.w("SRTParser", "Skip invalid SRT cue: $tL / $timeL") } } } } } } catch (e: Exception) { Log.e("SRTParser", "Parse SRT error", e); runOnUiThread { Toast.makeText(this, "SRT Parse Error", Toast.LENGTH_SHORT).show() } } finally { try { r.close() } catch (ioe: Exception) {} }; return c.sortedBy { it.startTimeMs } }

    // --- Time Parsing Helper ---
    private fun timeToMillis(t: String): Long? { try { val p=t.split(":"); val msS: String; val sS: String; val lP=p.last(); val dI=lP.indexOf('.'); if(dI!=-1){sS=lP.substring(0, dI); msS=lP.substring(dI+1)}else{ sS=lP; msS="0" }; val msDigits=msS.filter{it.isDigit()}; if(msDigits.isEmpty()){Log.w("TimeParser","Bad ms: $t"); return null}; val ms=msDigits.padEnd(3,'0').take(3).toLong(); if(sS.isEmpty()||sS.any{!it.isDigit()}){Log.w("TimeParser","Bad secs: $t"); return null}; val s=sS.toLong(); if(s<0||s>59){Log.w("TimeParser","Bad secs val: $t"); return null}; return when(p.size){3->{if(p[1].isEmpty()||p[1].any{!it.isDigit()}||p[0].isEmpty()||p[0].any{!it.isDigit()}){Log.w("TimeParser","Bad H/M: $t"); return null}; val m=p[1].toLong(); val h=p[0].toLong(); if(m<0||m>59){Log.w("TimeParser","Bad min val: $t"); return null}; (h*3600000+m*60000+s*1000+ms)}2->{if(p[0].isEmpty()||p[0].any{!it.isDigit()}){Log.w("TimeParser","Bad M: $t"); return null}; val m=p[0].toLong(); if(m<0||m>59){Log.w("TimeParser","Bad min val: $t"); return null}; (m*60000+s*1000+ms)}else->{Log.w("TimeParser","Bad colon#: $t"); null}}}catch(e:Exception){Log.e("TimeParser","Time parse err: $t",e); return null}}

    // --- Overlay Permission and Service Handling ---
    private fun handleLaunchOverlayClick() { Log.d(TAG, "Ensuring overlay service started."); if (checkOverlayPermission()) startOverlayService() else requestOverlayPermission() }
    private fun checkOverlayPermission(): Boolean { val has = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true; Log.d(TAG, "Overlay perm status: $has"); return has }
    private fun requestOverlayPermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { Log.d(TAG, "Requesting overlay perm."); Toast.makeText(this, "Need 'Draw over apps' permission.", Toast.LENGTH_LONG).show(); val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")); try { overlayPermissionLauncher.launch(i) } catch (e: Exception) { Log.e(TAG, "Can't launch overlay settings", e); Toast.makeText(this, "Can't open perm settings.", Toast.LENGTH_SHORT).show() } } }
    private fun startOverlayService() { if (!checkOverlayPermission()) { Log.w(TAG, "Start service denied (no perm)"); requestOverlayPermission(); return }; Log.d(TAG, "Starting OverlayService..."); val i = Intent(this, OverlayService::class.java); try { startService(i); sendStyleUpdate() } catch (e: Exception) { Log.e(TAG, "Can't start OverlayService", e); Toast.makeText(this, "Failed to start overlay.", Toast.LENGTH_SHORT).show() } }
    private fun stopOverlayService() { Log.d(TAG, "Stopping OverlayService..."); val i = Intent(this, OverlayService::class.java); stopService(i) }
    private fun sendSubtitleUpdate(text: String) { val textToSend = if (isOverlayUIShown) text else ""; if (textToSend.isNotBlank()) { Log.d(TAG, "Broadcasting update: '$textToSend'") } else { Log.d(TAG, "Broadcasting empty subtitle update.") }; val i = Intent(ACTION_UPDATE_SUBTITLE_LOCAL).apply { putExtra(EXTRA_SUBTITLE_TEXT_LOCAL, textToSend) }; LocalBroadcastManager.getInstance(this).sendBroadcast(i) }

    /** Broadcast the current caption style so the floating overlay restyles live. */
    private fun sendStyleUpdate() {
        val i = Intent(OverlayService.ACTION_UPDATE_STYLE).apply {
            putExtra(OverlayService.EXTRA_TEXT_SIZE, captionTextSizeSp)
            putExtra(OverlayService.EXTRA_TEXT_COLOR, captionTextColor)
            putExtra(OverlayService.EXTRA_BOX_COLOR, captionBoxColor)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(i)
    }

    // --- Playback Control ---
    private fun togglePlayPause() { if (isPlaying) pausePlayback() else startPlayback() }

    /** Recompute the clock origin from the current media position + speed. */
    private fun rebaseClock() {
        val speed = playbackSpeed
        startTimeNanos = System.nanoTime() - ((pausedElapsedTimeMillis / speed) * 1_000_000.0).toLong()
    }

    /** Current media-time position in ms, taking playback speed into account. */
    private fun currentMediaMs(): Long {
        val speed = playbackSpeed
        return ((System.nanoTime() - startTimeNanos) / 1_000_000.0 * speed).toLong()
    }

    /** Apply the keep-screen-on window flag based on the user setting + playback state. */
    private fun applyKeepScreenFlag() {
        if (isPlaying && keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startPlayback() {
        if (subtitleCues.isEmpty()) return
        isPlaying = true
        setPlayButtonState(true) // Set icon to Pause
        applyKeepScreenFlag()

        rebaseClock()
        if (isOverlayUIShown) { val c = findCueForTime(pausedElapsedTimeMillis); sendSubtitleUpdate(c?.text ?: "") } else { sendSubtitleUpdate("") }
        handler.post(updateRunnable)
    }

    private fun pausePlayback() {
        if (!isPlaying) return
        isPlaying = false
        setPlayButtonState(false) // Set icon to Play
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Update paused time only when actually pausing
        pausedElapsedTimeMillis = currentMediaMs()
        handler.removeCallbacks(updateRunnable)
    }

    private fun resetPlayback() {
        if (isPlaying) { handler.removeCallbacks(updateRunnable); isPlaying = false }
        pausedElapsedTimeMillis = 0L
        startTimeNanos = 0L
        currentCueIndex = -1
        isOverlayUIShown = true // Reset overlay visibility state
        setCaptionText("[Select VTT / SRT File]") // Updated default text
        textViewCurrentTime.text = formatTime(0)
        setPlayButtonState(false) // Set icon to Play
        val cuesLoaded = subtitleCues.isNotEmpty()
        buttonPlayPause.isEnabled = cuesLoaded
        buttonReset.isEnabled = cuesLoaded
        buttonLaunchOverlay.isEnabled = cuesLoaded
        buttonPrevCue.isEnabled = cuesLoaded
        buttonNextCue.isEnabled = cuesLoaded
        seekButtons.forEach { it.isEnabled = cuesLoaded }
        sliderPlayback.value = 0.0f // Reset Slider value
        sliderPlayback.isEnabled = cuesLoaded
        sendSubtitleUpdate("") // Clear the overlay text

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Playback reset, screen allowed to turn off.")
    }

    private fun resetPlaybackStateOnError() {
        subtitleCues = emptyList(); selectedFileUri = null
        buttonPlayPause.isEnabled = false; buttonReset.isEnabled = false; buttonLaunchOverlay.isEnabled = false
        buttonPrevCue.isEnabled = false; buttonNextCue.isEnabled = false
        seekButtons.forEach { it.isEnabled = false }
        sliderPlayback.value = 0.0f // Reset Slider value
        sliderPlayback.isEnabled = false
        isOverlayUIShown = true; setCaptionText("[Error loading file]"); textViewCurrentTime.text = formatTime(0)
        if (!textViewFilePath.text.startsWith("File:")) { textViewFilePath.text = "No file or error" }
        sendSubtitleUpdate("")

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Error state reset, screen allowed to turn off.")
    }

    private fun skipToCue(delta: Int) {
        if (subtitleCues.isEmpty()) return
        val now = if (isPlaying) currentMediaMs() else pausedElapsedTimeMillis
        val target = if (delta > 0) {
            // Next: first cue that starts strictly after current position
            subtitleCues.indexOfFirst { it.startTimeMs > now }.takeIf { it >= 0 } ?: return
        } else {
            // Prev: step back by index
            when {
                currentCueIndex < 0 -> 0
                else -> (currentCueIndex - 1).coerceAtLeast(0)
            }
        }
        currentCueIndex = target
        pausedElapsedTimeMillis = subtitleCues[target].startTimeMs
        textViewCurrentTime.text = formatTime(pausedElapsedTimeMillis)
        val cue = subtitleCues[target]
        setCaptionText(cue.text)
        sendSubtitleUpdate(cue.text)
        rebaseClock()
        val sliderVal = pausedElapsedTimeMillis.toFloat().coerceIn(sliderPlayback.valueFrom, sliderPlayback.valueTo)
        if (!sliderPlayback.isPressed) sliderPlayback.value = sliderVal
        if (!isPlaying) setPlayButtonState(false)
    }

    private fun seekBy(deltaMs: Long) {
        if (subtitleCues.isEmpty()) return
        val now = if (isPlaying) currentMediaMs() else pausedElapsedTimeMillis
        val target = (now + deltaMs).coerceIn(0L, sliderPlayback.valueTo.toLong())
        pausedElapsedTimeMillis = target
        textViewCurrentTime.text = formatTime(target)
        val cue = findCueForTime(target)
        val cueText = cue?.text ?: ""
        setCaptionText(cueText)
        sendSubtitleUpdate(cueText)
        rebaseClock()
        if (!sliderPlayback.isPressed) {
            sliderPlayback.value = target.toFloat().coerceIn(sliderPlayback.valueFrom, sliderPlayback.valueTo)
        }
        if (!isPlaying) setPlayButtonState(false)
    }

    // Helper function to change Play/Pause button icon and text (both windowed + fullscreen)
    private fun setPlayButtonState(playing: Boolean) {
        val label = if (playing) "Pause" else "Play"
        val icon = ContextCompat.getDrawable(this, if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow)
        buttonPlayPause.text = label
        buttonPlayPause.icon = icon
        if (::buttonFsPlayPause.isInitialized) {
            buttonFsPlayPause.text = label
            buttonFsPlayPause.icon = ContextCompat.getDrawable(this, if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow)
        }
    }

    /** Update the caption text in both the windowed view and the fullscreen view. */
    private fun setCaptionText(text: String) {
        textViewSubtitle.text = text
        textViewFullscreenSubtitle.text = text
    }

    // --- Subtitle Display Update Logic ---
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            val elapsedMillis = currentMediaMs()
            textViewCurrentTime.text = formatTime(elapsedMillis)

            // Update Slider only if user isn't dragging it
            // Also check bounds to prevent crash if time slightly exceeds max due to timing
            if (!sliderPlayback.isPressed) {
                if (elapsedMillis.toFloat() >= sliderPlayback.valueFrom && elapsedMillis.toFloat() <= sliderPlayback.valueTo) {
                    sliderPlayback.value = elapsedMillis.toFloat()
                } else if (elapsedMillis.toFloat() > sliderPlayback.valueTo) {
                    // If time exceeded max, clamp slider value to max
                    sliderPlayback.value = sliderPlayback.valueTo
                }
            }

            val activeCue = findCueForTime(elapsedMillis)
            if (activeCue != null) currentCueIndex = subtitleCues.indexOf(activeCue)
            val newText = activeCue?.text ?: ""
            var textChanged = false
            if (textViewSubtitle.text != newText) { setCaptionText(newText); textChanged = true }
            if (textChanged || (activeCue == null && newText == "")) { sendSubtitleUpdate(newText) }

            if (subtitleCues.isNotEmpty()) {
                val lastCueEndTime = subtitleCues.last().endTimeMs + postRollMs
                if (elapsedMillis >= lastCueEndTime) {
                    // Call pausePlayback first to handle flags/state/button icon
                    pausePlayback()
                    // Set final UI state after pausing
                    textViewCurrentTime.text = formatTime(lastCueEndTime)
                    if (!sliderPlayback.isPressed) { sliderPlayback.value = sliderPlayback.valueTo }
                    setCaptionText("[Playback Finished]")
                    sendSubtitleUpdate("[Playback Finished]")
                    return // Stop runnable
                }
            } else { pausePlayback(); sendSubtitleUpdate(""); return } // Safety check
            handler.postDelayed(this, 50) // Update ~20 times per second
        }
    }

    // --- Find Cue Logic (with pre-roll / post-roll padding) ---
    private fun findCueForTime(elapsedMillis: Long): SubtitleCue? {
        val pre = preRollMs
        val post = postRollMs
        return subtitleCues.find { c ->
            elapsedMillis >= (c.startTimeMs - pre) && elapsedMillis < (c.endTimeMs + post)
        }
    }

    // --- Format Time Logic ---
    private fun formatTime(millis: Long): String {
        val t = millis.coerceAtLeast(0)
        val ms = t % 1000
        val totalSec = t / 1000
        val s = totalSec % 60
        val m = (totalSec / 60) % 60
        val h = totalSec / 3600
        return when (timeFormatIndex) {
            AppSettings.TIME_FORMAT_MMSS    -> String.format("%02d:%02d", m + h * 60, s)
            AppSettings.TIME_FORMAT_HMMSSMS -> String.format("%d:%02d:%02d.%03d", h, m, s, ms)
            AppSettings.TIME_FORMAT_HMMSS   -> String.format("%d:%02d:%02d", h, m, s)
            else                            -> String.format("%02d:%02d.%03d", m + h * 60, s, ms)
        }
    }

    // --- Caption Styling ---
    private fun applyCaptionStyle() {
        val size = captionTextSizeSp
        val textColor = captionTextColor
        val boxColor = captionBoxColor
        textViewSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        textViewSubtitle.setTextColor(textColor)
        textViewSubtitle.setBackgroundColor(boxColor)
        // Fullscreen caption uses the same color, scaled up for presentation.
        textViewFullscreenSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * 1.6f)
        textViewFullscreenSubtitle.setTextColor(textColor)
    }

    // --- Fullscreen Mode ---
    private fun enterFullscreen() {
        isFullscreen = true
        fullscreenContainer.setBackgroundColor(fullscreenBgColor)
        applyCaptionStyle()
        fullscreenControls.visibility = View.GONE
        fullscreenContainer.visibility = View.VISIBLE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitFullscreen() {
        isFullscreen = false
        fullscreenContainer.visibility = View.GONE
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun toggleFullscreenControls() {
        fullscreenControls.visibility = if (fullscreenControls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    // --- Settings Bottom Sheet ---
    private fun showSettingsSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_settings, null)
        dialog.setContentView(view)

        val sliderTextSize = view.findViewById<Slider>(R.id.sliderTextSize)
        val labelTextSize = view.findViewById<TextView>(R.id.labelTextSize)
        val rowTextColor = view.findViewById<LinearLayout>(R.id.rowTextColor)
        val rowBoxColor = view.findViewById<LinearLayout>(R.id.rowBoxColor)
        val sliderSpeed = view.findViewById<Slider>(R.id.sliderSpeed)
        val labelSpeed = view.findViewById<TextView>(R.id.labelSpeed)
        val switchKeep = view.findViewById<SwitchMaterial>(R.id.switchKeepScreenOn)
        val sliderPreRoll = view.findViewById<Slider>(R.id.sliderPreRoll)
        val labelPreRoll = view.findViewById<TextView>(R.id.labelPreRoll)
        val sliderPostRoll = view.findViewById<Slider>(R.id.sliderPostRoll)
        val labelPostRoll = view.findViewById<TextView>(R.id.labelPostRoll)
        val radioGroupTimeFormat = view.findViewById<RadioGroup>(R.id.radioGroupTimeFormat)
        val textTimeFormatExample = view.findViewById<TextView>(R.id.textTimeFormatExample)
        val rowFsBg = view.findViewById<LinearLayout>(R.id.rowFullscreenBg)
        val buttonDone = view.findViewById<MaterialButton>(R.id.buttonSettingsDone)

        // Text size
        sliderTextSize.value = captionTextSizeSp.coerceIn(AppSettings.MIN_TEXT_SIZE, AppSettings.MAX_TEXT_SIZE)
        labelTextSize.text = "${getString(R.string.setting_text_size)} (${sliderTextSize.value.toInt()}sp)"
        sliderTextSize.addOnChangeListener { _, value, _ ->
            captionTextSizeSp = value
            labelTextSize.text = "${getString(R.string.setting_text_size)} (${value.toInt()}sp)"
            applyCaptionStyle(); sendStyleUpdate()
        }

        // Playback speed (capture position under old speed before applying)
        sliderSpeed.value = playbackSpeed.coerceIn(AppSettings.MIN_SPEED, AppSettings.MAX_SPEED)
        labelSpeed.text = "${getString(R.string.setting_playback_speed)} (${sliderSpeed.value}x)"
        sliderSpeed.addOnChangeListener { _, value, _ ->
            if (isPlaying) { pausedElapsedTimeMillis = currentMediaMs() }
            playbackSpeed = value
            if (isPlaying) { rebaseClock() }
            labelSpeed.text = "${getString(R.string.setting_playback_speed)} (${value}x)"
        }

        // Keep screen awake
        switchKeep.isChecked = keepScreenOn
        switchKeep.setOnCheckedChangeListener { _, checked ->
            keepScreenOn = checked
            applyKeepScreenFlag()
        }

        // Pre-roll
        sliderPreRoll.value = preRollMs.toFloat().coerceIn(0f, AppSettings.MAX_ROLL_MS.toFloat())
        labelPreRoll.text = "${getString(R.string.setting_pre_roll)} (${"%.1f".format(sliderPreRoll.value / 1000f)}s)"
        sliderPreRoll.addOnChangeListener { _, value, _ ->
            preRollMs = value.toLong()
            labelPreRoll.text = "${getString(R.string.setting_pre_roll)} (${"%.1f".format(value / 1000f)}s)"
        }

        // Post-roll
        sliderPostRoll.value = postRollMs.toFloat().coerceIn(0f, AppSettings.MAX_ROLL_MS.toFloat())
        labelPostRoll.text = "${getString(R.string.setting_post_roll)} (${"%.1f".format(sliderPostRoll.value / 1000f)}s)"
        sliderPostRoll.addOnChangeListener { _, value, _ ->
            postRollMs = value.toLong()
            labelPostRoll.text = "${getString(R.string.setting_post_roll)} (${"%.1f".format(value / 1000f)}s)"
        }

        // Time format
        val radioIds = listOf(
            R.id.radioTimeMMSSms, R.id.radioTimeMMSS, R.id.radioTimeHMMSSms, R.id.radioTimeHMMSS
        )
        radioGroupTimeFormat.check(radioIds[timeFormatIndex.coerceIn(0, 3)])
        val sampleMs = 225678L // 3:45.678 / 0:03:45.678
        fun refreshExample() {
            val pos = if (subtitleCues.isNotEmpty()) {
                if (isPlaying) currentMediaMs() else pausedElapsedTimeMillis
            } else sampleMs
            textTimeFormatExample.text = "Current time: ${formatTime(pos)}"
        }
        refreshExample()
        radioGroupTimeFormat.setOnCheckedChangeListener { _, checkedId ->
            timeFormatIndex = radioIds.indexOf(checkedId).coerceAtLeast(0)
            refreshExample()
            textViewCurrentTime.text = formatTime(if (isPlaying) currentMediaMs() else pausedElapsedTimeMillis)
        }

        // Color swatch rows
        buildSwatchRow(rowTextColor, AppSettings.TEXT_COLOR_SWATCHES, captionTextColor) {
            captionTextColor = it; applyCaptionStyle(); sendStyleUpdate()
        }
        buildSwatchRow(rowBoxColor, AppSettings.BOX_COLOR_SWATCHES, captionBoxColor) {
            captionBoxColor = it; applyCaptionStyle(); sendStyleUpdate()
        }
        buildSwatchRow(rowFsBg, AppSettings.BG_COLOR_SWATCHES, fullscreenBgColor) {
            fullscreenBgColor = it
            if (isFullscreen) fullscreenContainer.setBackgroundColor(it)
        }

        buttonDone.setOnClickListener { dialog.dismiss() }

        // Open fully expanded and scrolled to the top (otherwise a tall sheet opens
        // in the collapsed peek state, showing the bottom of the content).
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            view.scrollTo(0, 0)
        }
        dialog.show()
    }

    /** Populate a row with tappable color swatches; the selected one gets a highlight ring. */
    private fun buildSwatchRow(row: LinearLayout, colors: IntArray, current: Int, onPick: (Int) -> Unit) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        val size = (40 * density).toInt()
        val margin = (6 * density).toInt()
        colors.forEach { color ->
            val v = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = margin
            v.layoutParams = lp
            v.background = makeSwatchDrawable(color, color == current)
            v.setOnClickListener {
                onPick(color)
                buildSwatchRow(row, colors, color, onPick) // refresh selection ring
            }
            row.addView(v)
        }
    }

    private fun makeSwatchDrawable(color: Int, selected: Boolean): GradientDrawable {
        val density = resources.displayMetrics.density
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = 8 * density
        d.setColor(color)
        val strokeW = ((if (selected) 3 else 1) * density).toInt()
        d.setStroke(strokeW, if (selected) Color.WHITE else Color.GRAY)
        return d
    }

} // End of MainActivity class
