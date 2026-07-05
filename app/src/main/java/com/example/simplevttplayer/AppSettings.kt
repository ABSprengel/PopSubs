package com.example.simplevttplayer

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * Single source of truth for user-configurable settings, persisted via SharedPreferences.
 * Read/written by MainActivity (in-app caption + fullscreen) and OverlayService (floating overlay).
 */
object AppSettings {

    private const val PREFS_NAME = "popsubs_prefs"

    private const val KEY_TEXT_SIZE = "caption_text_size_sp"
    private const val KEY_TEXT_COLOR = "caption_text_color"
    private const val KEY_BOX_COLOR = "caption_box_color"
    private const val KEY_SPEED = "playback_speed"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_PRE_ROLL = "pre_roll_ms"
    private const val KEY_POST_ROLL = "post_roll_ms"
    private const val KEY_FULLSCREEN_BG = "fullscreen_bg_color"
    private const val KEY_TIME_FORMAT = "time_format"

    // Defaults chosen to match the app's original hardcoded look.
    const val DEFAULT_TEXT_SIZE = 18f
    val DEFAULT_TEXT_COLOR = Color.WHITE
    val DEFAULT_BOX_COLOR = Color.parseColor("#CC000000")
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_KEEP_SCREEN_ON = true
    const val DEFAULT_PRE_ROLL = 0L
    const val DEFAULT_POST_ROLL = 0L
    val DEFAULT_FULLSCREEN_BG = Color.BLACK

    // Bounds used by the settings sliders.
    const val MIN_TEXT_SIZE = 12f
    const val MAX_TEXT_SIZE = 48f
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2.0f
    const val MAX_ROLL_MS = 3000L

    // Time format indices (stored in prefs as Int)
    const val TIME_FORMAT_MMSSMS = 0  // 01:23.456
    const val TIME_FORMAT_MMSS   = 1  // 01:23
    const val TIME_FORMAT_HMMSSMS = 2 // 1:01:23.456
    const val TIME_FORMAT_HMMSS  = 3  // 1:01:23

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var Context.captionTextSizeSp: Float
        get() = prefs(this).getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
        set(value) = prefs(this).edit().putFloat(KEY_TEXT_SIZE, value).apply()

    var Context.captionTextColor: Int
        get() = prefs(this).getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
        set(value) = prefs(this).edit().putInt(KEY_TEXT_COLOR, value).apply()

    var Context.captionBoxColor: Int
        get() = prefs(this).getInt(KEY_BOX_COLOR, DEFAULT_BOX_COLOR)
        set(value) = prefs(this).edit().putInt(KEY_BOX_COLOR, value).apply()

    var Context.playbackSpeed: Float
        get() = prefs(this).getFloat(KEY_SPEED, DEFAULT_SPEED)
        set(value) = prefs(this).edit().putFloat(KEY_SPEED, value).apply()

    var Context.keepScreenOn: Boolean
        get() = prefs(this).getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON)
        set(value) = prefs(this).edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var Context.preRollMs: Long
        get() = prefs(this).getLong(KEY_PRE_ROLL, DEFAULT_PRE_ROLL)
        set(value) = prefs(this).edit().putLong(KEY_PRE_ROLL, value).apply()

    var Context.postRollMs: Long
        get() = prefs(this).getLong(KEY_POST_ROLL, DEFAULT_POST_ROLL)
        set(value) = prefs(this).edit().putLong(KEY_POST_ROLL, value).apply()

    var Context.fullscreenBgColor: Int
        get() = prefs(this).getInt(KEY_FULLSCREEN_BG, DEFAULT_FULLSCREEN_BG)
        set(value) = prefs(this).edit().putInt(KEY_FULLSCREEN_BG, value).apply()

    var Context.timeFormatIndex: Int
        get() = prefs(this).getInt(KEY_TIME_FORMAT, TIME_FORMAT_MMSSMS)
        set(value) = prefs(this).edit().putInt(KEY_TIME_FORMAT, value).apply()

    /** Preset swatches offered in the settings sheet for text / box / background colors. */
    val TEXT_COLOR_SWATCHES = intArrayOf(
        Color.WHITE, Color.YELLOW, Color.parseColor("#00E5FF"),
        Color.parseColor("#69F0AE"), Color.parseColor("#FF8A80"), Color.BLACK
    )

    val BOX_COLOR_SWATCHES = intArrayOf(
        Color.TRANSPARENT, Color.parseColor("#CC000000"), Color.parseColor("#99000000"),
        Color.parseColor("#CC0D47A1"), Color.parseColor("#CC1B5E20"), Color.parseColor("#CC4A148C")
    )

    val BG_COLOR_SWATCHES = intArrayOf(
        Color.BLACK, Color.parseColor("#1A1A1A"), Color.parseColor("#0D47A1"),
        Color.parseColor("#1B5E20"), Color.parseColor("#4A148C"), Color.parseColor("#3E2723")
    )
}
