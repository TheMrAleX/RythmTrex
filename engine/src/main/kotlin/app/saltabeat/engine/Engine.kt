package app.saltabeat.engine

import kotlin.math.*
import kotlin.random.Random

enum class Difficulty(val initial: Double, val cap: Double, val window: Double) {
    EASY(90.0, 130.0, 1.2), NORMAL(100.0, 160.0, 1.0), HARD(115.0, 180.0, .85)
}
enum class Kind(val width: Double, val height: Double) { SPIKES(60.0,24.0), BOX(40.0,40.0), WALL(56.0,72.0) }
enum class Judgment(val points: Int) { PERFECT(300), GREAT(200), GOOD(100), EARLY(0), MISS(0) }

/** Continuous tempo; each stage is 64 beats. The first eight beats ramp linearly in time. */
class Tempo(val difficulty: Difficulty, val fixedBpm: Double? = null, val practiceStage: Int = 1) {
    private val starts = DoubleArray(64)
    private val ramps = DoubleArray(64)
    private val rates = DoubleArray(64)
    private var capped = 0
    init {
        rates[0] = fixedBpm ?: difficulty.initial
        for (s in 1..63) {
            val old = rates[s-1]
            val next = fixedBpm ?: min(difficulty.cap, difficulty.initial + 4*s)
            rates[s] = next
            ramps[s] = if (next == old) 0.0 else 8*60 / ((old+next)/2)
            val previousDuration = if (s == 1) 64*60/old else ramps[s-1] + 56*60/old
            starts[s] = starts[s-1] + if (s > 1 && ramps[s-1] == 0.0) 64*60/old else previousDuration
            if (next == (fixedBpm ?: difficulty.cap) && capped == 0) capped = s
        }
        if (fixedBpm != null) capped = 1
    }
    private fun stageIndex(t: Double): Int {
        var i=0
        while (i < 63 && starts[i+1] <= t) i++
        return i
    }
    fun beatAt(time: Double): Double {
        val t=max(0.0,time)
        val i=stageIndex(t); val local=t-starts[i]; val ramp=ramps[i]
        return i*64.0 + if (ramp>0 && local<ramp) {
            val old=rates[i-1]; (old*local+(rates[i]-old)*local*local/(2*ramp))/60
        } else if (ramp>0) 8+(local-ramp)*rates[i]/60 else local*rates[i]/60
    }
    fun timeAt(beat: Double): Double {
        val i=min(63, max(0, floor(beat/64).toInt())); val b=beat-i*64
        val ramp=ramps[i]
        return starts[i]+if (ramp>0 && b<8) {
            val a=(rates[i]-rates[i-1])/(2*ramp)
            (-rates[i-1]+sqrt(rates[i-1]*rates[i-1]+4*a*b*60))/(2*a)
        } else if (ramp>0) ramp+(b-8)*60/rates[i] else b*60/rates[i]
    }
    fun bpmAt(t: Double): Double {
        val i=stageIndex(max(0.0,t)); val local=t-starts[i]; val ramp=ramps[i]
        return if(ramp>0 && local<ramp) rates[i-1]+(rates[i]-rates[i-1])*local/ramp else rates[i]
    }
    fun stageAt(t: Double): Int = if(fixedBpm != null) practiceStage else floor(beatAt(t)/64).toInt()+1
    fun airtime(t: Double) = (60/bpmAt(t)).coerceIn(.45,.7)
}

data class Note(val id: Int, val hit: Double, val encounter: Double, val kind: Kind,
                val x: Double, val y: Double, val part: Int, val air: Double, val speed: Double) {
    var judgment: Judgment? = null
    var collided = false
    fun obstacleX(time: Double, runnerX: Double) = runnerX + speed*(encounter-time)
}

/** At most two simultaneous circles: 1.1s approach / >=.6s spacing. */
class Patterns(seed: Long, val tempo: Tempo, width: Double = 360.0, height: Double = 240.0) {
    private val random=Random(seed)
    private var beat=4.0
    private var lastHit=-10.0
    private var index=0
    private var pending: Note?=null
    private val width=width.coerceAtLeast(320.0)
    private val height=height.coerceAtLeast(240.0)
    private fun note(at: Double, kind: Kind, part: Int): Note {
        // Alternating diagonal halves leave room for two generous hit regions.
        val side=index%2
        val inset=random.nextDouble(0.0, 8.0)
        val x=if(side==0) 56+inset else width-56-inset
        val y=if(side==0) 56+inset else height-56-inset
        val air=tempo.airtime(at)
        return Note(index++,at,at+air/2,kind,x,y,part,air,280*tempo.bpmAt(at)/100)
    }
    fun next(): Note {
        pending?.let { pending=null;lastHit=it.hit;return it }
        var at=tempo.timeAt(beat)
        val stage=tempo.stageAt(at)
        val gap=when(stage) { 1->4.0; 2->2.0; 3->if(random.nextBoolean()) 2.0 else 1.5; else->1+random.nextDouble() }
        at=max(at,lastHit+.65) // >.5s leaves calibration-independent input recovery.
        val kind=if(stage>=3 && random.nextInt(5)==0) Kind.WALL else if(random.nextBoolean()) Kind.SPIKES else Kind.BOX
        val n=note(at,kind,if(kind==Kind.WALL)1 else 0)
        if(kind==Kind.WALL) {
            val second=at+max(.65,30/tempo.bpmAt(at))
            pending=note(second,kind,2)
            beat=tempo.beatAt(second)+gap
        } else beat=tempo.beatAt(at)+gap
        lastHit=at
        return n
    }
}

object Judge {
    fun judge(delta: Double, difficulty: Difficulty): Judgment? {
        val d=abs(delta); val m=difficulty.window; val eps=1e-9
        return when {
            d <= .045*m+eps -> Judgment.PERFECT
            d <= .090*m+eps -> Judgment.GREAT
            d <= .140*m+eps -> Judgment.GOOD
            delta >= -.300-eps && delta<0 -> Judgment.EARLY
            delta > .140*m -> Judgment.MISS
            else -> null
        }
    }
}

data class Jump(val start: Double, val encounter: Double, val duration: Double) {
    /** Asymmetric parabola when the minimum duration would move the apex past the obstacle. */
    fun heightAt(time: Double): Double {
        if(time<=start || time>=start+duration) return 0.0
        val rise=max(.001,encounter-start)
        val f=if(time<=encounter) (time-start)/rise else (start+duration-time)/max(.001,start+duration-encounter)
        return 96*(1-(1-f.coerceIn(0.0,1.0)).pow(2))
    }
}

class Score(val practice: Boolean=false) {
    var points=0; private set
    var combo=0; private set
    var bestCombo=0; private set
    var lives=3; private set
    var perfects=0; private set
    var hits=0; private set
    var attempts=0; private set
    var invincibleUntil=-1.0; private set
    val multiplier get()=min(5,1+combo/10)
    val accuracy get()=if(attempts==0) 0.0 else hits*100.0/attempts
    fun hit(j: Judgment): Boolean {
        require(j.points>0)
        attempts++;hits++;combo++;bestCombo=max(bestCombo,combo)
        if(j==Judgment.PERFECT) perfects++
        points+=j.points*multiplier
        val gained=combo%25==0 && lives<3
        if(combo%25==0) lives=min(3,lives+1)
        return gained
    }
    fun breakCombo() { combo=0;attempts++ }
    fun collide(time: Double): Boolean {
        if(practice || time<invincibleUntil) return false
        lives=max(0,lives-1);invincibleUntil=time+1.2;return true
    }
}

/** Maps Android event uptime to song time; calibration is applied exactly once. */
object InputClock {
    fun songTime(eventUptimeMs: Long, anchorUptimeMs: Long, anchorSong: Double, calibrationMs: Int): Double =
        anchorSong+(eventUptimeMs-anchorUptimeMs)/1000.0-calibrationMs.coerceIn(-300,300)/1000.0
}
object Calibration {
    fun estimate(offsetsMs: List<Double>): Int {
        if(offsetsMs.isEmpty()) return 0
        val s=offsetsMs.sorted();val median=(s[(s.size-1)/2]+s[s.size/2])/2
        val clean=s.filter{abs(it-median)<=80}
        val m=(clean[(clean.size-1)/2]+clean[clean.size/2])/2
        return m.roundToInt().coerceIn(-300,300)
    }
}

class Game(seed: Long, val difficulty: Difficulty, practiceBpm: Double?=null, practiceStage: Int=1,
           width: Double=360.0, padHeight: Double=240.0) {
    val tempo=Tempo(difficulty,practiceBpm,practiceStage)
    private val patterns=Patterns(seed,tempo,width,padHeight)
    val notes=ArrayList<Note>(32)
    val score=Score(practiceBpm!=null)
    var jump: Jump?=null; private set
    var feedback: Judgment?=null; private set
    var feedbackTime=-10.0; private set
    var feedbackX=0.0; private set
    var feedbackY=0.0; private set
    var lifeGained=false; private set
    var time=0.0; private set
    val over get()=score.lives==0
    private var next=patterns.next()
    init { notes.add(next);next=patterns.next();advance(0.0) }
    fun advance(songTime: Double) {
        if(over) return
        time=max(time,songTime)
        while(next.hit<time+3) { notes.add(next);next=patterns.next() }
        for(n in notes) {
            if(n.judgment==null && time>n.hit+.140*difficulty.window+1e-9) {
                n.judgment=Judgment.MISS;score.breakCombo();show(n,Judgment.MISS,time)
            }
            if(!n.collided && time>=n.encounter && n.judgment?.points==0) {
                n.collided=true;score.collide(n.encounter)
            }
        }
        while(notes.isNotEmpty() && notes[0].encounter<time-2) notes.removeAt(0)
    }
    private fun show(n: Note,j: Judgment,t: Double) {feedback=j;feedbackTime=t;feedbackX=n.x;feedbackY=n.y}
    fun tap(songTime: Double,x: Double,y: Double): Judgment? {
        if(over) return null
        var best: Note?=null; var distance=Double.MAX_VALUE
        for(n in notes) {
            if(n.judgment!=null || songTime<n.hit-1.1 || songTime>n.hit+.140*difficulty.window) continue
            if((n.x-x).pow(2)+(n.y-y).pow(2)>64*64) continue
            val delta=abs(songTime-n.hit)
            if(delta<distance) {best=n;distance=delta}
        }
        val n=best?:return null
        val j=Judge.judge(songTime-n.hit,difficulty)?:return null
        n.judgment=j
        if(j.points>0) {
            lifeGained=score.hit(j)
            jump=Jump(songTime,n.encounter,max(.5*n.air,2*(n.encounter-songTime)))
        } else {score.breakCombo();lifeGained=false}
        show(n,j,songTime)
        return j
    }
}
