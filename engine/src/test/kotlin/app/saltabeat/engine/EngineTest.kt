package app.saltabeat.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class EngineTest {
    @Test fun judgmentBoundaries() {
        for(d in Difficulty.entries) {
            for(sign in listOf(-1,1)) {
                assertEquals(Judgment.PERFECT,Judge.judge(sign*.045*d.window,d))
                assertEquals(Judgment.GREAT,Judge.judge(sign*.090*d.window,d))
                assertEquals(Judgment.GOOD,Judge.judge(sign*.140*d.window,d))
            }
            assertEquals(Judgment.EARLY,Judge.judge(-.300,d))
            assertNull(Judge.judge(-.30001,d))
            assertEquals(Judgment.EARLY,Judge.judge(-.140*d.window-.00001,d))
            assertEquals(Judgment.MISS,Judge.judge(.140*d.window+.00001,d))
        }
    }
    @Test fun scoringAndLives() {
        val s=Score();repeat(9){s.hit(Judgment.GOOD)};assertEquals(900,s.points)
        s.hit(Judgment.PERFECT);assertEquals(1500,s.points);assertEquals(2,s.multiplier)
        assertTrue(s.collide(1.0));assertFalse(s.collide(2.0));assertEquals(2,s.lives)
        repeat(15){s.hit(Judgment.GREAT)};assertEquals(3,s.lives)
        repeat(100){s.hit(Judgment.GOOD)};assertEquals(5,s.multiplier);assertEquals(3,s.lives)
        s.breakCombo();assertEquals(0,s.combo)
        assertTrue(s.collide(3.0));assertTrue(s.collide(4.2));assertTrue(s.collide(5.4));assertEquals(0,s.lives)
        val p=Score(true);repeat(20){p.collide(it*2.0)};assertEquals(3,p.lives)
    }
    @Test fun determinismAndThousandRunsSpacing() {
        for(seed in 0L until 1000L) {
            val tempo=Tempo(Difficulty.entries[(seed%3).toInt()])
            val a=Patterns(seed,tempo,320.0,240.0);val b=Patterns(seed,tempo,320.0,240.0)
            val history=ArrayList<Note>();var last:Note?=null
            repeat(600) {
                val n=a.next();assertEquals(n,b.next())
                last?.let { assertTrue(n.hit-it.hit>=.5-1e-9); assertTrue(hypot(n.x-it.x,n.y-it.y)>40) }
                for(p in history) if(n.hit-p.hit<1.1+.168) assertTrue(hypot(n.x-p.x,n.y-p.y)>=150)
                history.removeAll{n.hit-it.hit>2};history.add(n);last=n
                assertTrue(n.x in 56.0..264.0);assertTrue(n.y in 56.0..184.0)
            }
        }
    }
    @Test fun tempoIsContinuousAndInvertible() {
        val t=Tempo(Difficulty.HARD)
        for(i in 0..10000) {
            val time=i/10.0;val b=t.beatAt(time)
            assertEquals(time,t.timeAt(b),1e-7)
            assertTrue(t.bpmAt(time) in 115.0..180.0)
        }
        assertEquals(180.0,t.bpmAt(10000.0),0.0)
    }
    @Test fun timestampAndCalibrationExtremes() {
        assertEquals(10.042,InputClock.songTime(1042,1000,10.0,0),1e-9)
        assertEquals(9.742,InputClock.songTime(1042,1000,10.0,300),1e-9)
        assertEquals(10.342,InputClock.songTime(1042,1000,10.0,-300),1e-9)
        assertEquals(42,Calibration.estimate(listOf(40.0,42.0,44.0,41.0,42.0,43.0,250.0,-200.0)))
    }
    @Test fun perfectBotSurvivesTenMinutesHardAtEveryCalibration() {
        for(calibration in listOf(-300,0,300)) {
            val g=Game(9821,Difficulty.HARD)
            var t=0.0;var hits=0
            while(t<600) {
                g.advance(t)
                for(n in g.notes) if(n.judgment==null && n.hit<=t+.01) {
                    val eventMs=((n.hit+calibration/1000.0)*1000).roundToLong()
                    val input=InputClock.songTime(eventMs,0,0.0,calibration)
                    assertEquals(Judgment.PERFECT,g.tap(input,n.x,n.y));hits++
                }
                assertEquals(3,g.score.lives);assertFalse(g.over);t+=.01
            }
            assertTrue(hits>400);assertEquals(hits,g.score.perfects)
        }
    }
    @Test fun successfulJumpApexAndVisibleClearance() {
        for(d in Difficulty.entries) for(bpm in listOf(70.0,d.cap)) {
            val g=Game(5,d,bpm,4)
            val n=g.notes.first()
            for(delta in listOf(-.140*d.window,0.0,.140*d.window)) {
                val start=n.hit+delta
                val j=Jump(start,n.encounter,max(.5*n.air,2*(n.encounter-start)))
                assertEquals(96.0,j.heightAt(n.encounter),1e-8)
                assertTrue(j.heightAt(n.encounter)>Kind.WALL.height)
                assertEquals(0.0,j.heightAt(start),0.0)
            }
        }
    }
    @Test fun missesDamageOnceAndOutsideTapsIgnored() {
        val g=Game(1,Difficulty.NORMAL)
        assertNull(g.tap(0.0,0.0,0.0));assertEquals(0,g.score.attempts)
        g.advance(g.notes.first().encounter+.001)
        assertEquals(2,g.score.lives)
        g.advance(g.time+.001);assertEquals(2,g.score.lives)
    }
}
