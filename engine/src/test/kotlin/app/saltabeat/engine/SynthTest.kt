package app.saltabeat.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SynthTest {
    @Test fun pcmIsDeterministicBoundedAndNonSilent() {
        val tempo=Tempo(Difficulty.HARD,180.0,4)
        val a=Synth(tempo,false,42);val b=Synth(tempo,false,42)
        var power=0.0
        for(i in 0 until 44100) {
            val sample=a.sample(i/44100.0,1f,1f)
            assertEquals(sample,b.sample(i/44100.0,1f,1f),0.0)
            assertTrue(sample.isFinite() && abs(sample)<1)
            power+=sample*sample
        }
        assertTrue(sqrt(power/44100)>.03)
    }
    @Test fun musicAndEffectsHaveIndependentGains() {
        val s=Synth(Tempo(Difficulty.NORMAL),false,1)
        repeat(1000){assertEquals(0.0,s.sample(it/44100.0,0f,0f),0.0)}
        s.trigger(1)
        assertTrue(abs(s.sample(1.0,0f,1f))>0 || abs(s.sample(1.001,0f,1f))>0)
    }
}
