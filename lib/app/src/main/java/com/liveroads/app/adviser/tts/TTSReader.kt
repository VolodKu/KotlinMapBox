package com.liveroads.app.adviser.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

object TTSReader {
    private lateinit var _contextRef: WeakReference<Context>

    private var context: Context?
        get() = _contextRef.get()
        set(value) {
            _contextRef.clear()
            _contextRef = WeakReference(value!!)
        }

    private lateinit var tts: TextToSpeech

    fun init(_context: Context) {
        _contextRef = WeakReference(_context)
        tts = TextToSpeech(context, object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {

            }
        })
    }

    fun sayNow(text: String) {
        tts.language = Locale.UK
        //todo: respect API
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, HashMap(0))
    }

    fun sayNext(text: String) {
        tts.language = Locale.UK
        //todo: respect API
        tts.speak(text, TextToSpeech.QUEUE_ADD, HashMap(0))
    }

    fun isSpeaking(): Boolean = tts.isSpeaking

    fun stop() {
        tts.stop()
    }
}
