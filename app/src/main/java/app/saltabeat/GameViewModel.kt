package app.saltabeat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.saltabeat.audio.BeatAudio
import app.saltabeat.data.*
import app.saltabeat.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class Screen { HOME, DIFFICULTY, PRACTICE, COUNTDOWN, GAME, PAUSE, OVER, TUTORIAL, CALIBRATION, SETTINGS, ABOUT, CLEAR }
data class MenuState(val screen:Screen=Screen.HOME,val settings:Settings=Settings(),val loaded:Boolean=false,
                     val difficulty:Difficulty=Difficulty.NORMAL,val practiceBpm:Int=100,val practiceStage:Int=1,
                     val countdown:Int=3,val tutorialStep:Int=0,val calibrationCount:Int=0,val adjustment:Int=0,
                     val newRecord:Boolean=false)
class GameViewModel(app:Application):AndroidViewModel(app) {
    private val prefs=Preferences(app)
    private val mutable=MutableStateFlow(MenuState())
    val state=mutable.asStateFlow()
    val audio=BeatAudio()
    var game:Game?=null;private set
    var haptic:((Boolean)->Unit)?=null
    var width=360.0
    var padHeight=240.0
    private var task:Job?=null
    private var practice=false
    private var seed=0L
    private var pausedSong=0.0
    private var calibrationReturn=Screen.SETTINGS
    private val offsets=ArrayList<Double>(8)
    private var lastCalibrationBeat=-1
    init { viewModelScope.launch {
        prefs.settings.collect{s->
            val first=!mutable.value.loaded
            mutable.update{it.copy(settings=s,loaded=true,difficulty=if(first)s.difficulty else it.difficulty,adjustment=s.calibration)}
            audio.musicVolume=s.music;audio.effectsVolume=s.effects
            if(first && !s.calibrated) calibrate(Screen.HOME)
        }
    } }
    fun navigate(screen:Screen) {
        task?.cancel()
        if(state.value.screen==Screen.CALIBRATION) audio.stop()
        if(screen==Screen.HOME) audio.stop()
        mutable.update{it.copy(screen=screen)}
    }
    fun select(d:Difficulty) {mutable.update{it.copy(difficulty=d)};viewModelScope.launch{prefs.difficulty(d)};newRun(false)}
    fun practiceBpm(bpm:Int) {mutable.update{it.copy(practiceBpm=bpm.coerceIn(70,180))}}
    fun practiceStage(stage:Int) {mutable.update{it.copy(practiceStage=stage.coerceIn(1,4))}}
    fun newRun(isPractice:Boolean=practice) {
        task?.cancel();audio.stop();practice=isPractice;seed=System.nanoTime()
        val s=state.value
        game=Game(seed,s.difficulty,if(practice)s.practiceBpm.toDouble() else null,s.practiceStage,width,padHeight)
        if(!s.settings.tutorial) {mutable.update{it.copy(screen=Screen.TUTORIAL,tutorialStep=0)};return}
        countdown(false)
    }
    fun tutorialNext() {
        if(state.value.tutorialStep<2) mutable.update{it.copy(tutorialStep=it.tutorialStep+1)}
        else tutorialDone()
    }
    fun tutorialDone() {viewModelScope.launch{prefs.toggle("tutorial",true)};countdown(false)}
    private fun countdown(resuming:Boolean) {
        task?.cancel()
        mutable.update{it.copy(screen=Screen.COUNTDOWN,countdown=3)}
        val tempo=game?.tempo?:return
        audio.start(Tempo(state.value.difficulty,100.0),clicksOnly=true)
        task=viewModelScope.launch {
            for(i in 0..3) {
                mutable.update{it.copy(countdown=3-i)};audio.effect(2)
                while(audio.time()<(i+1)*.6) delay(8)
            }
            audio.start(tempo,if(resuming)pausedSong else 0.0,seed=seed)
            mutable.update{it.copy(screen=Screen.GAME)}
        }
    }
    fun tick() {
        if(state.value.screen!=Screen.GAME) return
        val g=game?:return
        val before=g.feedbackTime
        val stage=g.tempo.stageAt(g.time)
        g.advance(audio.time())
        if(g.tempo.stageAt(g.time)>stage) audio.effect(4)
        if(g.feedbackTime!=before && g.feedback==Judgment.MISS) audio.effect(3)
        if(g.over) {
            audio.pause()
            val record=g.score.points>state.value.settings.record(state.value.difficulty)
            mutable.update{it.copy(screen=Screen.OVER,newRecord=record && !practice)}
            if(!practice) viewModelScope.launch{prefs.record(state.value.difficulty,g.score.points,g.score.bestCombo)}
        }
    }
    fun tap(uptimeMs:Long,x:Double,y:Double) {
        val s=state.value
        if(s.screen!=Screen.GAME) return
        val g=game?:return
        val j=g.tap(audio.eventTime(uptimeMs,s.settings.calibration),x,y)?:return
        audio.effect(if(j.points==0)3 else if(j==Judgment.PERFECT)1 else 2)
        if(g.lifeGained) audio.effect(4)
        if(s.settings.vibration) haptic?.invoke(j==Judgment.PERFECT)
    }
    fun pause() {
        if(state.value.screen==Screen.GAME || state.value.screen==Screen.COUNTDOWN) {
            task?.cancel();pausedSong=game?.time?:0.0;audio.pause();mutable.update{it.copy(screen=Screen.PAUSE)}
        } else if(state.value.screen==Screen.CALIBRATION) {
            audio.stop();task?.cancel();mutable.update{it.copy(screen=calibrationReturn)}
        }
    }
    fun resume()=countdown(true)
    fun calibrate(returnTo:Screen=Screen.SETTINGS) {
        task?.cancel();calibrationReturn=returnTo;offsets.clear();lastCalibrationBeat=-1
        mutable.update{it.copy(screen=Screen.CALIBRATION,calibrationCount=0,adjustment=it.settings.calibration)}
        audio.start(Tempo(Difficulty.NORMAL,100.0),clicksOnly=true)
    }
    fun calibrationTap(uptimeMs:Long) {
        if(offsets.size>=8) return
        val t=audio.eventTime(uptimeMs,0)
        val beat=((t-1.2)/.6).toInt().let{ kotlin.math.round((t-1.2)/.6).toInt() }
        if(beat !in 0..7 || beat==lastCalibrationBeat || kotlin.math.abs(t-(1.2+beat*.6))>.3) return
        lastCalibrationBeat=beat;offsets.add((t-(1.2+beat*.6))*1000)
        mutable.update{it.copy(calibrationCount=offsets.size)}
        if(offsets.size==8) {
            audio.pause();mutable.update{it.copy(adjustment=Calibration.estimate(offsets))}
        }
    }
    fun adjust(delta:Int) {mutable.update{it.copy(adjustment=(it.adjustment+delta).coerceIn(-300,300))}}
    fun saveCalibration() {val ms=state.value.adjustment;viewModelScope.launch{prefs.calibration(ms)};navigate(calibrationReturn)}
    fun skipCalibration() {viewModelScope.launch{prefs.toggle("calibrated",true)};navigate(calibrationReturn)}
    fun music(v:Float) {audio.musicVolume=v;viewModelScope.launch{prefs.music(v)}}
    fun effects(v:Float) {audio.effectsVolume=v;viewModelScope.launch{prefs.effects(v)}}
    fun toggle(key:String,value:Boolean) {viewModelScope.launch{prefs.toggle(key,value)}}
    fun clearRecords() {viewModelScope.launch{prefs.clear()};navigate(Screen.SETTINGS)}
    override fun onCleared() {audio.stop();super.onCleared()}
}
