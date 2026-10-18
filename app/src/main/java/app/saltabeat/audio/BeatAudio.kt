package app.saltabeat.audio

import android.media.*
import android.os.Process
import android.os.SystemClock
import app.saltabeat.engine.Tempo
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

/** Single presentation clock. All PCM (music, metronome and effects) shares this stream. */
class BeatAudio {
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running=false
    @Volatile var musicVolume=.65f
    @Volatile var effectsVolume=.8f
    @Volatile private var offset=0.0
    private var baseNanos=0L
    private var frozen=0.0
    private val timestamp=AudioTimestamp()
    private var lastHead=0L
    private var wraps=0L
    @Volatile private var written=0L
    private val effect=AtomicInteger(0)
    private var resumeTempo: Tempo?=null
    private var metronome=false
    fun effect(kind: Int) { effect.set(kind) }
    @Synchronized fun time(): Double {
        if(!running) return frozen
        val a=track
        val elapsed=if(a!=null) {
            if(a.getTimestamp(timestamp)) {
                val projected=timestamp.framePosition+(System.nanoTime()-timestamp.nanoTime)*44100.0/1e9
                projected.coerceIn(0.0,written.toDouble())/44100
            } else {
                val h=a.playbackHeadPosition.toLong() and 0xffffffffL
                if(h<lastHead) wraps+=1L shl 32
                lastHead=h
                (h+wraps).toDouble()/44100
            }
        } else (System.nanoTime()-baseNanos)/1e9
        return max(frozen,offset+elapsed)
    }
    fun eventTime(uptimeMs: Long,calibrationMs: Int): Double =
        app.saltabeat.engine.InputClock.songTime(uptimeMs,SystemClock.uptimeMillis(),time(),calibrationMs)
    @Synchronized fun start(tempo: Tempo, from: Double=0.0, clicksOnly: Boolean=false, seed: Long=0L) {
        stop()
        resumeTempo=tempo;metronome=clicksOnly
        offset=from;frozen=from;lastHead=0;wraps=0;written=0
        baseNanos=System.nanoTime();running=true
        try {
            val min=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)
            track=AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(max(1764,min))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
            track!!.play()
            val active=track!!
            thread=Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val synth=Synth(tempo,clicksOnly,seed)
                val pcm=ShortArray(256)
                var frames=0L
                while(running) {
                    val fx=effect.getAndSet(0)
                    if(fx!=0) synth.trigger(fx)
                    for(i in pcm.indices) pcm[i]=(synth.sample(offset+(frames+i)/44100.0,musicVolume,effectsVolume)*30000).toInt().coerceIn(-32768,32767).toShort()
                    var sent=0
                    while(sent<pcm.size && running) {
                        val n=active.write(pcm,sent,pcm.size-sent,AudioTrack.WRITE_BLOCKING)
                        if(n<=0) break
                        sent+=n
                    }
                    if(sent==0) break
                    frames+=sent;written=frames
                }
            },"Saltabeat PCM").also{it.start()}
        } catch (_: IllegalArgumentException) {track?.release();track=null}
          catch (_: IllegalStateException) {track?.release();track=null}
          catch (_: UnsupportedOperationException) {track?.release();track=null}
    }
    @Synchronized fun pause() { if(running) {frozen=time();stopStream()} }
    @Synchronized fun resume() {resumeTempo?.let{start(it,frozen,metronome)}}
    @Synchronized fun stop() {if(running) frozen=time();stopStream()}
    private fun stopStream() {
        running=false
        track?.pause()
        thread?.join(500);thread=null
        track?.flush();track?.release();track=null
    }
}

/** Original procedural score. Envelopes derive from absolute song phase, never a frame delta. */
private class Synth(private val tempo: Tempo,private val clicksOnly: Boolean,seed:Long) {
    private var noise=0x7a51
    private var fx=0
    private var fxAge=100.0
    private val chords=arrayOf(intArrayOf(45,48,52,43),intArrayOf(48,55,57,53),intArrayOf(50,53,57,48),intArrayOf(43,47,50,45))
    private val progression=chords[kotlin.random.Random(seed).nextInt(4)]
    private val arpNotes=intArrayOf(0,3,7,12)
    private var previousNoise=0.0
    fun trigger(kind: Int) {fx=kind;fxAge=0.0}
    private fun random():Double {noise=noise xor (noise shl 13);noise=noise xor (noise ushr 17);noise=noise xor (noise shl 5);return noise.toDouble()/Int.MAX_VALUE}
    private fun hz(midi:Int)=440*2.0.pow((midi-69)/12.0)
    fun sample(t: Double,music: Float,effects: Float): Double {
        val beat=tempo.beatAt(t)
        val phase=beat-floor(beat)
        val second=phase*60/tempo.bpmAt(t)
        val beatIndex=floor(beat).toInt()
        val stage=tempo.stageAt(t)
        val n=random()
        val high=n-previousNoise;previousNoise=n
        var out=0.0
        if(clicksOnly) {
            if(beatIndex in 2..9 && second<.035) out=sin(2*PI*1100*second)*exp(-second*100)*.6
        } else {
            if(second<.12) out+=sin(2*PI*(45*second+105*.024*(1-exp(-second/.024))))*exp(-second*30)*.6
            val half=beat*2-floor(beat*2)
            if(half<.09) out+=high*exp(-half*40)*.16
            val root=progression[(floor(beat/16).toInt()%4+4)%4]
            if(stage>=2) {
                val f=hz(root)
                val saw=2*((t*f)%1)-1
                // Weighted fundamental removes the harsh high harmonics of the raw saw.
                out+=(sin(2*PI*f*t)*.75+saw*.25)*exp(-phase*2)*.20
            }
            if(stage>=3) {
                if(beatIndex%4==1 || beatIndex%4==3) if(second<.09) out+=(n*.8+sin(2*PI*180*second)*.2)*exp(-second*40)*.24
                val arp=arpNotes[floor(beat*2).toInt() and 3]
                val f=hz(root+24+arp)
                val square=if((t*f)%1<.25) .18 else -.06
                out+=square*exp(-half*6)*.4
            }
            if(stage>=4 && beatIndex%8==7) out+=sin(2*PI*hz(root+19)*t)*exp(-phase*12)*.07
        }
        var sfx=0.0
        if(fxAge<.35) {
            val f=when(fx) {1->1320.0;2->880.0;3->70.0;4->660+floor(fxAge/.07)*220;else->880.0}
            sfx=sin(2*PI*f*fxAge)*exp(-fxAge*(if(fx==4)9 else 30))*.25
            fxAge+=1.0/44100
        }
        return tanh(out*music+sfx*effects)
    }
}
