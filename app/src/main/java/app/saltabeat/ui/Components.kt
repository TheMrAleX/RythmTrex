package app.saltabeat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import app.saltabeat.R

val Sky=Color(0xFF1A1033)
val Surface=Color(0xFF231544)
val SurfaceHigh=Color(0xFF2E1C58)
val Accent=Color(0xFF2EE6C5)
val Gold=Color(0xFFFFD166)
val Miss=Color(0xFFFF5A6E)
val Muted=Color.White.copy(alpha=.65f)
val Rounded=FontFamily(Font(R.font.fredoka,FontWeight.Black),Font(R.font.fredoka,FontWeight.Medium))

@Composable fun Label(text:String,size:Int=16,color:Color=Color.White,modifier:Modifier=Modifier,weight:FontWeight=FontWeight.Black) {
    Text(text,modifier,color=color,fontSize=size.sp,fontFamily=Rounded,fontWeight=weight,
        style=LocalTextStyle.current.copy(fontFeatureSettings="tnum"))
}

@Composable fun Chunky(text:String,variant:Int=0,modifier:Modifier=Modifier,onClick:()->Unit) {
    val interaction=remember{MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val down by animateDpAsState(if(pressed)4.dp else 0.dp,tween(80),label="press")
    val haptic=LocalHapticFeedback.current
    val fill=when(variant){0->Accent;1->Color.White;else->Color.Transparent}
    val ink=if(variant==2)Color.White else Sky
    Box(modifier.fillMaxWidth().heightIn(min=60.dp).padding(bottom=4.dp)
        .background(if(variant==2)Color.Transparent else fill.copy(red=fill.red*.75f,green=fill.green*.75f,blue=fill.blue*.75f),RoundedCornerShape(50))
        .offset(y=down).clip(RoundedCornerShape(50)).background(fill)
        .then(if(variant==2)Modifier.border(2.dp,Color.White.copy(alpha=.4f),RoundedCornerShape(50)) else Modifier)
        .clickable(interactionSource=interaction,indication=null,role=Role.Button){haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove);onClick()}
        .padding(horizontal=20.dp,vertical=16.dp),contentAlignment=Alignment.Center) {
        Label(text,18,ink)
    }
}
@Composable fun ToggleRow(title:String,value:Boolean,onChange:(Boolean)->Unit) {
    val status=stringResource(if(value)R.string.on else R.string.off)
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clip(RoundedCornerShape(12.dp))
        .semantics{stateDescription=status}.toggleableCompat(value,onChange).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
        Label(title,16,modifier=Modifier.weight(1f))
        Box(Modifier.size(52.dp,32.dp).background(if(value)Accent else SurfaceHigh,RoundedCornerShape(50)).padding(4.dp)) {
            Box(Modifier.align(if(value)Alignment.CenterEnd else Alignment.CenterStart).size(24.dp).background(Color.White,RoundedCornerShape(50)))
        }
    }
}
private fun Modifier.toggleableCompat(value:Boolean,onChange:(Boolean)->Unit)=this.semantics{role=Role.Switch}.clickable{onChange(!value)}

@Composable fun VolumeRow(title:String,value:Float,onChange:(Float)->Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)) {
        Label(title,16)
        // Custom rail and thumb. Tap/drag share the same normalized value.
        BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp).semantics {
            contentDescription=title
            progressBarRangeInfo=ProgressBarRangeInfo(value,0f..1f)
            setProgress {onChange(it.coerceIn(0f,1f));true}
        }) {
            val density=androidx.compose.ui.platform.LocalDensity.current
            val widthPx=with(density){maxWidth.toPx()}
            val currentOnChange by rememberUpdatedState(onChange)
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().pointerInput(widthPx){
                awaitPointerEventScope {
                    while(true) {
                        val event=awaitPointerEvent()
                        for(change in event.changes) if(change.pressed) {currentOnChange((change.position.x/widthPx).coerceIn(0f,1f));change.consume()}
                    }
                }
            }) {
                drawLine(SurfaceHigh,androidx.compose.ui.geometry.Offset(0f,center.y),androidx.compose.ui.geometry.Offset(size.width,center.y),8.dp.toPx(),androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(Accent,androidx.compose.ui.geometry.Offset(0f,center.y),androidx.compose.ui.geometry.Offset(size.width*value,center.y),8.dp.toPx(),androidx.compose.ui.graphics.StrokeCap.Round)
                drawCircle(Color.White,12.dp.toPx(),androidx.compose.ui.geometry.Offset((size.width*value).coerceIn(12.dp.toPx(),size.width-12.dp.toPx()),center.y))
            }
        }
    }
}
@Composable fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Surface).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
