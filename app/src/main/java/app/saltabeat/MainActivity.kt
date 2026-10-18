package app.saltabeat

import android.app.Activity
import android.media.*
import android.os.*
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.saltabeat.ui.SaltabeatApp

class MainActivity:ComponentActivity() {
    private val model:GameViewModel by viewModels()
    private lateinit var manager:AudioManager
    private lateinit var focus:AudioFocusRequest
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.attributes=window.attributes.apply{preferredRefreshRate=120f}
        manager=getSystemService(AudioManager::class.java)
        focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener{if(it<0)model.pause()}.build()
        model.haptic={strong->
            val vibrator=if(Build.VERSION.SDK_INT>=31)getSystemService(VibratorManager::class.java).defaultVibrator else getSystemService(android.os.Vibrator::class.java)
            vibrator.vibrate(VibrationEffect.createOneShot(if(strong)28 else 12,VibrationEffect.DEFAULT_AMPLITUDE))
        }
        setContent { SaltabeatApp(model){playing->
            if(playing) {window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);manager.requestAudioFocus(focus)}
            else {window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);manager.abandonAudioFocusRequest(focus)}
        } }
    }
    override fun onStop() {model.pause();manager.abandonAudioFocusRequest(focus);super.onStop()}
    override fun onDestroy() {model.haptic=null;super.onDestroy()}
}
