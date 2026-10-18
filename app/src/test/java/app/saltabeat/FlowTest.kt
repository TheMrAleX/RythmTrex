package app.saltabeat

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.saltabeat.audio.BeatAudio
import app.saltabeat.engine.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class FlowTest {
    private class Audio:BeatAudio() {
        var now=0.0
        override fun time()=now
        override fun start(tempo:Tempo,from:Double,clicksOnly:Boolean,seed:Long){now=from}
        override fun stop(){}
        override fun pause(){}
        override fun effect(kind:Int){}
        override fun eventTime(uptimeMs:Long,calibrationMs:Int)=uptimeMs/1000.0-calibrationMs/1000.0
    }
    private fun await(condition:()->Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idleFor(20,TimeUnit.MILLISECONDS)
            if(condition())return
            Thread.sleep(10)
        }
        assertTrue("Timed out waiting for persisted/menu state",condition())
    }
    private fun countdown(vm:GameViewModel,a:Audio,resuming:Boolean=false) {
        val duration=60/vm.game!!.tempo.bpmAt(vm.game!!.time)
        for(i in 1..if(resuming)3 else 4) {
            a.now=i*duration+.001
            shadowOf(Looper.getMainLooper()).idleFor(30,TimeUnit.MILLISECONDS)
        }
        assertEquals(Screen.GAME,vm.state.value.screen)
    }
    @Test fun fullRunPauseRetryCalibrationSettingsAndRestoration() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val audio=Audio();val vm=GameViewModel(app,audio)
        await{vm.state.value.loaded}
        if(vm.state.value.screen==Screen.CALIBRATION) vm.skipCalibration()
        vm.select(Difficulty.HARD)
        if(vm.state.value.screen==Screen.TUTORIAL) {
            assertEquals(0,vm.state.value.tutorialStep);vm.tutorialNext();vm.tutorialNext();vm.tutorialDone()
        }
        assertEquals(Screen.COUNTDOWN,vm.state.value.screen);countdown(vm,audio)
        val n=vm.game!!.notes.first()
        audio.now=n.hit;vm.tick();vm.tap((n.hit*1000).roundToLong(),n.x,n.y)
        assertEquals(1,vm.game!!.score.perfects)
        vm.pause();assertEquals(Screen.PAUSE,vm.state.value.screen)
        val frozen=vm.game!!.time
        vm.resume();countdown(vm,audio,true)
        assertEquals(frozen,audio.time(),1e-9)
        while(vm.state.value.screen==Screen.GAME){audio.now+=1;vm.tick()}
        assertEquals(Screen.OVER,vm.state.value.screen)
        assertEquals(0,vm.game!!.score.lives)
        await{vm.state.value.settings.tutorial && vm.state.value.settings.hard>0}
        vm.newRun();assertEquals(Screen.COUNTDOWN,vm.state.value.screen)
        vm.pause();vm.navigate(Screen.HOME);assertEquals(Screen.HOME,vm.state.value.screen)
        vm.navigate(Screen.SETTINGS);vm.toggle("reduce",true);vm.music(.25f);vm.effects(.4f)
        await{vm.state.value.settings.reduce && vm.state.value.settings.music==.25f}
        vm.calibrate()
        for(i in 0..7)vm.calibrationTap(((1.2+i*.6+.042)*1000).roundToLong())
        assertEquals(8,vm.state.value.calibrationCount);assertEquals(42,vm.state.value.adjustment)
        vm.saveCalibration();await{vm.state.value.settings.calibration==42}
        val restored=GameViewModel(app,Audio())
        await{restored.state.value.loaded}
        assertEquals(Screen.HOME,restored.state.value.screen)
        assertTrue(restored.state.value.settings.reduce)
        assertEquals(42,restored.state.value.settings.calibration)
        val store=androidx.lifecycle.ViewModelStore();store.put("original",vm);store.put("restored",restored);store.clear()
    }
}
