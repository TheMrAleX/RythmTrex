package app.saltabeat.audio

import android.media.*
import android.os.Process
import android.os.SystemClock
import app.saltabeat.engine.Tempo
import app.saltabeat.engine.Synth
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

/** Single presentation clock. All PCM (music, metronome and effects) shares this stream. */
open class BeatAudio {
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running=false
    @Volatile var musicVolume=.65f
    @Volatile var effectsVolume=.8f
    @Volatile private var offset=0.0
    private var baseNanos=0L
    private var frozen=0.0
    private var lastTime=0.0
    private var lastObserved=0.0
    private var lastProgressNanos=0L
    private var lastPollNanos=0L
    private val timestamp=AudioTimestamp()
    private var lastHead=0L
    private var wraps=0L
    @Volatile private var written=0L
    private val effect=AtomicInteger(0)
    private var resumeTempo: Tempo?=null
    private var metronome=false
    open fun effect(kind: Int) { effect.set(kind) }
    @Synchronized open fun time(): Double {
        if(!running) return frozen
        val a=track
        val now=System.nanoTime()
        val elapsed=try { if(a!=null) {
            if(a.getTimestamp(timestamp)) {
                val projected=timestamp.framePosition+(System.nanoTime()-timestamp.nanoTime)*44100.0/1e9
                projected.coerceIn(0.0,written.toDouble())/44100
            } else {
                val h=a.playbackHeadPosition.toLong() and 0xffffffffL
                if(h<lastHead) wraps+=1L shl 32
                lastHead=h
                (h+wraps).toDouble()/44100
            }
        } else (now-baseNanos)/1e9 } catch(_:IllegalStateException) { (now-baseNanos)/1e9 }
        if(elapsed>lastObserved) {lastObserved=elapsed;lastProgressNanos=now}
        val stalled=now-lastProgressNanos>300_000_000L
        val seconds=if(stalled)lastTime-offset+(now-lastPollNanos)/1e9 else elapsed
        lastPollNanos=now
        lastTime=max(lastTime,offset+seconds)
        return lastTime
    }
    open fun eventTime(uptimeMs: Long,calibrationMs: Int): Double =
        app.saltabeat.engine.InputClock.songTime(uptimeMs,SystemClock.uptimeMillis(),time(),calibrationMs)
    @Synchronized open fun start(tempo: Tempo, from: Double=0.0, clicksOnly: Boolean=false, seed: Long=0L) {
        stop()
        resumeTempo=tempo;metronome=clicksOnly
        offset=from;frozen=from;lastTime=from;lastHead=0;wraps=0;written=0
        baseNanos=System.nanoTime();lastProgressNanos=baseNanos;lastPollNanos=baseNanos;lastObserved=0.0;running=true
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
    @Synchronized open fun pause() { if(running) {frozen=time();stopStream()} }
    @Synchronized open fun resume() {resumeTempo?.let{start(it,frozen,metronome)}}
    @Synchronized open fun stop() {if(running) frozen=time();stopStream()}
    private fun stopStream() {
        running=false
        try {track?.pause()} catch(_:IllegalStateException) {}
        thread?.join(500);thread=null
        try {track?.flush()} catch(_:IllegalStateException) {}
        track?.release();track=null
    }
}

