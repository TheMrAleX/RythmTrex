package app.saltabeat.engine

import kotlin.math.*

/** Original procedural score. Envelopes derive from absolute song phase, never a frame delta. */
class Synth(private val tempo: Tempo,private val clicksOnly: Boolean,private val seed:Long) {
    private var noise=0x7a51
    private val fxKinds=IntArray(4)
    private val fxAges=DoubleArray(4){100.0}
    private var nextVoice=0
    private val chords=arrayOf(intArrayOf(45,48,52,43),intArrayOf(48,55,57,53),intArrayOf(50,53,57,48),intArrayOf(43,47,50,45))
    private val progressionIndex=kotlin.random.Random(seed).nextInt(4)
    private val progression=chords[progressionIndex]
    private val arpNotes=intArrayOf(0,if(progressionIndex%2==0)3 else 4,7,12)
    private var bassLow=0.0
    private var snareLow=0.0
    private var previousNoise=0.0
    fun trigger(kind: Int) {fxKinds[nextVoice]=kind;fxAges[nextVoice]=0.0;nextVoice=(nextVoice+1)%4}
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
        snareLow+=.25*(n-snareLow)
        var out=0.0
        if(clicksOnly) {
            if(beatIndex in (if(seed==Long.MIN_VALUE)0..3 else 2..9) && second<.035) out=sin(2*PI*1100*second)*exp(-second*100)*.6
        } else {
            if(second<.12) out+=sin(2*PI*(45*second+105*.024*(1-exp(-second/.024))))*exp(-second*30)*.6
            val half=beat*2-floor(beat*2)
            val hatSeconds=half*30/tempo.bpmAt(t)
            if(hatSeconds<.03) out+=high*exp(-hatSeconds*160)*.16
            val root=progression[(floor(beat/16).toInt()%4+4)%4]
            if(stage>=2) {
                val f=hz(root)
                val saw=2*((t*f)%1)-1
                // One-pole low-pass gives the saw a round, soft bass timbre.
                bassLow+=.06*(saw-bassLow)
                out+=bassLow*exp(-phase*2)*.26
            }
            if(stage>=3) {
                if(beatIndex%4==1 || beatIndex%4==3) if(second<.09) out+=((n-snareLow)*.8+sin(2*PI*180*second)*.2)*exp(-second*40)*.24
                val arp=arpNotes[floor(beat*2).toInt() and 3]
                val f=hz(root+24+arp)
                val square=if((t*f)%1<.25) .18 else -.06
                out+=square*exp(-half*6)*.4
            }
            if(stage>=4 && beatIndex%8==7) out+=sin(2*PI*hz(root+19)*t)*exp(-phase*12)*.07
        }
        var sfx=0.0
        for(voice in 0..3) if(fxAges[voice]<.35) {
            val fx=fxKinds[voice];val fxAge=fxAges[voice]
            val f=when(fx) {1->1320.0;2->880.0;3->70.0;4->660+floor(fxAge/.07)*220;else->880.0}
            sfx+=sin(2*PI*f*fxAge)*exp(-fxAge*(if(fx==4)9 else 30))*.25
            fxAges[voice]+=1.0/44100
        }
        return tanh(out*(if(clicksOnly)effects else music)+sfx*effects)
    }
}
