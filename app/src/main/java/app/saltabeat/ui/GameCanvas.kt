package app.saltabeat.ui

import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.content.res.ResourcesCompat
import app.saltabeat.R
import app.saltabeat.audio.BeatAudio
import app.saltabeat.data.Settings
import app.saltabeat.engine.*
import kotlin.math.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable fun GameCanvas(game:Game?,audio:BeatAudio,settings:Settings,padHeight:Dp,active:Boolean,
                          attract:Boolean=false,record:Int=0,onFrame:()->Unit={},onTap:(Long,Double,Double)->Unit={_,_,_->}) {
    val density=LocalDensity.current
    val context=LocalContext.current
    val description=stringResource(R.string.pad_description)
    val statusTop=with(density){WindowInsets.statusBars.getTop(this).toDp().value}
    val judgments=arrayOf(stringResource(R.string.perfect),stringResource(R.string.great),stringResource(R.string.good),stringResource(R.string.early),stringResource(R.string.miss))
    val recordLabel=stringResource(R.string.record,"%s")
    val comboLabel=stringResource(R.string.combo,0).substringBefore('0')
    val stageLabel=stringResource(R.string.stage,0).substringBefore('0')
    val art=remember{Art(ResourcesCompat.getFont(context,R.font.fredoka),judgments,recordLabel,comboLabel,stageLabel,density.fontScale)}
    var canvasHeight by remember{mutableFloatStateOf(800f)}
    val tick=remember{mutableLongStateOf(0)}
    val currentFrame by rememberUpdatedState(onFrame)
    LaunchedEffect(active,attract) {
        if(active || attract) while(true) withFrameNanos{if(active)currentFrame();tick.longValue=it}
    }
    Canvas(Modifier.fillMaxSize().onSizeChanged{canvasHeight=it.height/density.density}.semantics{contentDescription=description}.pointerInteropFilter{e->
        if(active && (e.actionMasked==MotionEvent.ACTION_DOWN || e.actionMasked==MotionEvent.ACTION_POINTER_DOWN)) {
            val i=e.actionIndex
            onTap(e.eventTime,e.getX(i)/density.density.toDouble(),e.getY(i)/density.density.toDouble()-(canvasHeight-padHeight.value))
        }
        active
    }) {
        tick.longValue
        val unit=density.density
        val w=size.width/unit;val h=size.height/unit
        val time=if(attract)tick.longValue/1e9 else game?.time?:0.0
        scale(unit,pivot=Offset.Zero) {art.draw(this,w,h,padHeight.value,time,game,settings,record,attract,statusTop)}
    }
}

private class Art(typeface:android.graphics.Typeface?,private val words:Array<String>,private val recordLabel:String,
                  private val comboLabel:String,private val stageLabel:String,private val fontScale:Float) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.typeface=typeface;fontVariationSettings="'wght' 700";fontFeatureSettings="tnum"}
    private val path=Path()
    private val skyBrushes=arrayOf(
        Brush.verticalGradient(listOf(Sky,Color(0xFF4B1F6F)),endY=700f),
        Brush.verticalGradient(listOf(Sky,Color(0xFF6B2A7F)),endY=700f),
        Brush.verticalGradient(listOf(Sky,Color(0xFF8A2F7A)),endY=700f),
        Brush.verticalGradient(listOf(Sky,Color(0xFF2C3E91)),endY=700f))
    private val starsX=FloatArray(30){((it*71+37)%359)/360f}
    private val starsY=FloatArray(30){((it*47+19)%197)/240f}
    private val dx=FloatArray(12){cos(it*PI/6).toFloat()}
    private val dy=FloatArray(12){sin(it*PI/6).toFloat()}
    private val judgeColors=arrayOf(Gold,Accent,Color(0xFF8DA2FF),Miss,Miss)
    private val formatter=java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("es"))
    private var oldScore=-1;private var scoreText="0"
    private var oldRecord=-1;private var recordText=""
    private var oldCombo=-1;private var comboText=""
    private var oldMultiplier=-1;private var multiplierText="";private var multiplierChanged=-10.0
    private var oldStage=-1;private var stageText=""
    private fun DrawScope.text(text:String,x:Float,y:Float,size:Float,color:Color,center:Boolean=false) {
        paint.color=color.toArgb();paint.textSize=size*fontScale
        paint.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(text,x,y,paint)
    }
    fun draw(d:DrawScope,w:Float,h:Float,pad:Float,t:Double,game:Game?,settings:Settings,record:Int,attract:Boolean,statusTop:Float) = with(d) {
        val stage=game?.tempo?.stageAt(t)?:1
        val beat=game?.tempo?.beatAt(t)?:t*100/60
        val bpm=game?.tempo?.bpmAt(t)?:100.0
        val phase=(beat-floor(beat)).toFloat()
        val pulse=if(settings.reduce)0f else (1-phase*8).coerceIn(0f,1f)
        val ground=h-pad-64
        val padTop=h-pad
        val speed=(280*bpm/100).toFloat()
        val distance=beat*168
        val feedbackAge=(t-(game?.feedbackTime?:-10.0)).toFloat()
        val failed=game?.feedback?.points==0
        val shake=if(!settings.reduce && failed && feedbackAge in 0f.. .2f)sin(feedbackAge*110)*6*(1-feedbackAge/.2f) else 0f
        translate(left=shake) {
            drawRect(skyBrushes[(stage-1)%4],size=Size(w,h))
            if(stage>1 && game?.tempo?.fixedBpm==null && beat%64<8) {
                drawRect(skyBrushes[(stage-2)%4],size=Size(w,h),alpha=(1-beat%64/8).toFloat())
            }
            for(i in 0..29) {
                val x=((starsX[i]*w-distance*.1)%w+w).toFloat()%w
                drawCircle(Color.White.copy(alpha=(.25+.3*(.5+.5*sin(t*.6+i))).toFloat()),if(i%3==0)2f else 1f,Offset(x,starsY[i]*ground))
            }
            val sunX=w*.5f
            val sunY=ground*.47f
            val sunRadius=96*(1+pulse*.03f)
            drawCircle(if(stage%4==3)Color(0xFFFF5E78) else if(stage%4==0)Color(0xFFFFC857) else Color(0xFFFF7A59),sunRadius,Offset(sunX,sunY))
            drawCircle(Color(0xFFFFB36B),sunRadius-4,Offset(sunX,sunY),style=Stroke(2f))
            drawRect(Color(0xFF2A1650),Offset(0f,sunY),Size(w,ground-sunY))
            for(i in -1..12) {
                val bx=(((i*48-distance*.3)%(w+96)+w+96)%(w+96)).toFloat()-48
                val bh=24f+((i+10)*37%76)
                drawRoundRect(Color(0xFF2A1650),Offset(bx,sunY-bh),Size(40f,ground-sunY+bh),CornerRadius(8f))
                if(i%3==0)drawRect(Color(0xFF6C4BB5).copy(alpha=.25f),Offset(bx+14,sunY-bh+12),Size(4f,8f))
            }
            drawRect(Color(0xFF0E0A1F),Offset(0f,ground),Size(w,padTop-ground))
            drawLine(Color(0xFF6C4BB5),Offset(0f,ground),Offset(w,ground),4f)
            for(i in 0..20) {
                val xx=((i*32-distance)%(w+32)+w+32).toFloat()%(w+32)
                drawLine(Color(0xFF6C4BB5).copy(alpha=.35f),Offset(xx,ground+20+(i%3)*12),Offset(xx+12,ground+20+(i%3)*12),2f)
            }
            val runnerX=w*.22f
            val air=game?.tempo?.airtime(t)?:.6
            val mark=runnerX+speed*air.toFloat()/2
            for(i in 0..6)drawLine(Color.White.copy(alpha=.2f),Offset(mark,ground-i*10),Offset(mark,ground-i*10-5),2f)
            if(game!=null) for(n in game.notes) {
                // A success neutralizes the solid obstacle immediately; its fragments are drawn as feedback.
                if((n.judgment?.points?:0)>0)continue
                val x=n.obstacleX(t,runnerX.toDouble()).toFloat()
                if(x< -80 || x>w+80)continue
                obstacle(n.kind,x,ground,t)
            }
            val jump=if(attract)max(0.0,sin(beat*PI/4))*12 else game?.jump?.heightAt(t)?:0.0
            val y=ground-jump.toFloat()
            val invincible=game!=null && game.score.invincibleUntil>t
            val alpha=if(invincible && (t/.08).toInt()%2==0).3f else 1f
            drawOval(Color.Black.copy(alpha=.4f),Offset(runnerX-20+8*(jump/96).toFloat(),ground-3),Size(40-16*(jump/96).toFloat(),8f))
            val stretch=when {
                game?.jump!=null && t-game.jump!!.start in 0.0.. .08 ->1.15f
                game?.jump!=null && t-game.jump!!.start-game.jump!!.duration in 0.0.. .1 ->.85f
                else->1f
            }
            scale(1f,stretch,pivot=Offset(runnerX,y)) {
                val gold=Gold.copy(alpha=alpha)
                drawRoundRect(gold,Offset(runnerX-22,y-62),Size(44f,48f),CornerRadius(14f))
                drawRoundRect(Color(0xFFC9942A).copy(alpha=alpha),Offset(runnerX-19,y-30),Size(13f,10f),CornerRadius(5f))
                for(i in 0..1) {
                    val eyeX=runnerX+1+i*15
                    drawCircle(Color.White.copy(alpha=alpha),5f,Offset(eyeX,y-46))
                    drawCircle(Sky.copy(alpha=alpha),2.5f,Offset(eyeX+2,y-46))
                    val lift=if(floor(beat*2).toInt()%2==i && jump<1)4f else 0f
                    drawRoundRect(gold,Offset(runnerX-13+i*20,y-15-lift),Size(8f,14f),CornerRadius(3f))
                }
            }
            val landing=game?.jump?.let{t-it.start-it.duration}?:-1.0
            if(landing in 0.0.. .10)for(i in 0..2) {
                val age=landing.toFloat()
                drawCircle(Color(0xFF6C4BB5).copy(alpha=1-age/.1f),2f+age*8,Offset(runnerX-20+i*20+(i-1)*age*80,ground-age*50))
            }
            drawRoundRect(Color(0xFF120C26),Offset(0f,padTop),Size(w,pad+32),CornerRadius(32f))
            drawRoundRect(Color.White.copy(alpha=.10f+pulse*.25f),Offset(0f,padTop),Size(w,pad+32),CornerRadius(32f),style=Stroke(2f))
            for(x in 16..w.toInt() step 32)for(yDot in 32..pad.toInt() step 32)drawCircle(Color.White.copy(alpha=.04f),1.5f,Offset(x.toFloat(),padTop+yDot))
            for(i in 0..3)drawCircle(if(floor(beat).toInt()%4==i)if(i==0)Color.White else Accent else Color.White.copy(alpha=.2f),4f,Offset(w/2-24+i*16,padTop+16))
            if(game!=null)for(n in game.notes) {
                val remaining=n.hit-t
                if(n.judgment!=null || remaining>1.1 || remaining< -.168)continue
                val cx=n.x.toFloat();val cy=padTop+n.y.toFloat()
                val fade=((1.1-remaining)/.15).coerceIn(0.0,1.0).toFloat()
                val radius=36+72*(remaining/1.1).coerceIn(0.0,1.0).toFloat()
                scale(.8f+.2f*fade,pivot=Offset(cx,cy)) {
                drawCircle(Color.White.copy(alpha=.9f*fade),radius,Offset(cx,cy),style=Stroke(6f))
                drawCircle(Accent.copy(alpha=fade),36f,Offset(cx,cy))
                drawCircle(Color.White.copy(alpha=fade),36f,Offset(cx,cy),style=Stroke(3f))
                drawCircle(Color.White.copy(alpha=.35f*fade),12f,Offset(cx-12,cy-12))
                if(n.part>0)text(if(n.part==1)"1" else "2",cx,cy+7,20f,Sky,true)
                }
            }
            if(feedbackAge in 0f.. .6f && game?.feedback!=null) {
                val j=game.feedback!!.ordinal;val color=judgeColors[j]
                val x=game.feedbackX.toFloat();val yFeedback=padTop+game.feedbackY.toFloat()
                if(feedbackAge<.25f && !failed) {
                    drawCircle(color.copy(alpha=1-feedbackAge/.25f),36+44*feedbackAge/.25f,Offset(x,yFeedback),style=Stroke(4f))
                    for(i in 0..11)drawCircle(color.copy(alpha=1-feedbackAge/.25f),3f,Offset(x+dx[i]*feedbackAge*260,yFeedback+dy[i]*feedbackAge*260))
                }
                if(feedbackAge<.25f && failed) {
                    val fade=1-feedbackAge/.25f
                    drawCircle(Miss.copy(alpha=fade),18*fade,Offset(x-18-feedbackAge*40,yFeedback+feedbackAge*40))
                    drawCircle(Miss.copy(alpha=fade),18*fade,Offset(x+18+feedbackAge*40,yFeedback+feedbackAge*40))
                }
                text(words[j],w/2,padTop-18-feedbackAge*40,22f,color.copy(alpha=1-feedbackAge/.6f),true)
            }
            if(failed && !settings.reduce && feedbackAge in 0f.. .12f) drawRect(Miss.copy(alpha=.15f*(1-feedbackAge/.12f)),size=Size(w,h))
            if(!attract && game!=null) {
                val s=game.score
                if(oldScore!=s.points){oldScore=s.points;scoreText=formatter.format(s.points)}
                if(oldRecord!=record){oldRecord=record;recordText=recordLabel.replace("%s",formatter.format(record))}
                if(oldCombo!=s.combo){oldCombo=s.combo;comboText=comboLabel+s.combo}
                if(oldMultiplier!=s.multiplier){oldMultiplier=s.multiplier;multiplierText="x${s.multiplier}";multiplierChanged=t}
                if(oldStage!=stage){oldStage=stage;stageText=stageLabel+stage}
                text(scoreText,20f,statusTop+52f,if(w<340 && fontScale>1.1f)30f else 36f,Color.White)
                text(recordText,20f,statusTop+76f,13f,Muted)
                val age=t-multiplierChanged
                val bounce=if(!settings.reduce && age in 0.0.. .15)1f+.25f*sin(age/.15*PI).toFloat() else 1f
                scale(bounce,pivot=Offset(145f,statusTop+40f)) {
                    drawRoundRect(Accent,Offset(126f,statusTop+27f),Size(38f,26f),CornerRadius(13f))
                    text(multiplierText,145f,statusTop+46f,16f,Sky,true)
                }
                val heartsY=if(w<340 && fontScale>1.1f)statusTop+82f else statusTop+48f
                if(s.practice)text("∞",w-126,heartsY,24f,Accent)
                else for(i in 0..2)text(if(i<s.lives)"♥" else "♡",w-126+i*24,heartsY,24f,if(i<s.lives)Miss else Color.White.copy(alpha=.3f))
                if(s.combo>=5) {
                    val comboBounce=if(!settings.reduce && !failed && feedbackAge in 0f.. .15f)1+.15f*sin(feedbackAge/.15f*PI).toFloat() else 1f
                    scale(comboBounce,pivot=Offset(w/2,h*.24f)){text(comboText,w/2,h*.24f,18f,Gold,true)}
                }
                if(failed && feedbackAge in 0f.. .5f)text("♥",w-120,statusTop+76+feedbackAge*70,24f,Miss.copy(alpha=1-feedbackAge/.5f))
                val sinceStage=beat%64*60/bpm
                if(stage>1 && sinceStage<1.2) {
                    val slide=((sinceStage/1.2)*2-1).toFloat()
                    text(stageText,w/2+slide*w,ground*.4f,40f,Color.White,true)
                }
            }
        }
    }
    private fun DrawScope.obstacle(kind:Kind,x:Float,ground:Float,t:Double) {
        val c=when(kind){Kind.SPIKES->Miss;Kind.BOX->Color(0xFF5ED3F3);Kind.WALL->Color(0xFFB58CFF)}
        val dark=Color(c.red*.75f,c.green*.75f,c.blue*.75f)
        val width=kind.width.toFloat();val height=kind.height.toFloat()
        drawOval(Color.Black.copy(alpha=.3f),Offset(x-width/2,ground-4),Size(width,8f))
        if(kind==Kind.SPIKES) {
            for(i in 0..2) {
                val left=x-30+i*20
                path.reset();path.moveTo(left,ground);path.lineTo(left+10,ground-24);path.lineTo(left+20,ground);path.close()
                drawPath(path,c);drawPath(path,dark,style=Stroke(3f,join=StrokeJoin.Round))
            }
        } else {
            rotate(if(kind==Kind.BOX)sin(t*4).toFloat()*3 else 0f,pivot=Offset(x,ground)) {
                drawRoundRect(c,Offset(x-width/2,ground-height),Size(width,height),CornerRadius(8f))
                drawRoundRect(dark,Offset(x-width/2,ground-height),Size(width,height),CornerRadius(8f),style=Stroke(3f))
                if(kind==Kind.BOX)for(i in 0..1)drawCircle(dark,2.5f,Offset(x-8+i*16,ground-24))
                else for(i in 0..1)drawRoundRect(dark.copy(alpha=.4f),Offset(x-18,ground-50+i*22),Size(36f,8f),CornerRadius(4f))
            }
        }
    }
}
@Preview(widthDp=360,heightDp=800) @Composable private fun GamePreview(){GameCanvas(Game(1,Difficulty.NORMAL),remember{BeatAudio()},Settings(),272.dp,false)}
