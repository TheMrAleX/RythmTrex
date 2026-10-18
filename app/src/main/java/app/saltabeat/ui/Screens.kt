package app.saltabeat.ui

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saltabeat.*
import app.saltabeat.R
import app.saltabeat.data.Settings
import app.saltabeat.engine.*
import java.text.NumberFormat
import java.util.Locale

private fun number(n:Int)=NumberFormat.getIntegerInstance(Locale.forLanguageTag("es")).format(n)
@Composable fun difficultyName(d:Difficulty)=stringResource(when(d){Difficulty.EASY->R.string.easy;Difficulty.NORMAL->R.string.normal;Difficulty.HARD->R.string.hard})

@Composable fun SaltabeatApp(vm:GameViewModel,onPlaying:(Boolean)->Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val playing=s.screen==Screen.GAME || s.screen==Screen.COUNTDOWN || s.screen==Screen.CALIBRATION
    LaunchedEffect(playing){onPlaying(playing)}
    BackHandler(s.screen!=Screen.HOME) {
        when(s.screen) {
            Screen.GAME,Screen.COUNTDOWN->vm.pause()
            Screen.PAUSE,Screen.OVER,Screen.DIFFICULTY,Screen.PRACTICE->vm.navigate(Screen.HOME)
            Screen.CALIBRATION->vm.skipCalibration()
            Screen.TUTORIAL->vm.tutorialDone()
            else->vm.navigate(Screen.SETTINGS.takeIf{s.screen==Screen.ABOUT || s.screen==Screen.CLEAR}?:Screen.HOME)
        }
    }
    CompositionLocalProvider(LocalVibration provides s.settings.vibration) { MaterialTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Sky)) {
            val density=androidx.compose.ui.platform.LocalDensity.current
            val nav=with(density){WindowInsets.navigationBars.getBottom(this).toDp()}
            val pad=(maxHeight*.34f).coerceIn(240.dp,320.dp)
            SideEffect{vm.width=maxWidth.value.toDouble();vm.padHeight=(pad-nav).value.toDouble()}
            if(s.screen in listOf(Screen.HOME,Screen.DIFFICULTY,Screen.GAME,Screen.COUNTDOWN,Screen.PAUSE,Screen.OVER,Screen.TUTORIAL)) {
                GameCanvas(vm.game.takeIf{s.screen!=Screen.HOME && s.screen!=Screen.DIFFICULTY},vm.audio,s.settings,pad,
                    active=s.screen==Screen.GAME,attract=s.screen==Screen.HOME || s.screen==Screen.DIFFICULTY,
                    record=s.settings.record(s.difficulty),onFrame=vm::tick,onTap=vm::tap)
            }
            when(s.screen) {
                Screen.HOME->HomeScreen(s.settings.record(s.difficulty),{vm.navigate(Screen.DIFFICULTY)},{vm.navigate(Screen.PRACTICE)},{vm.navigate(Screen.SETTINGS)})
                Screen.DIFFICULTY->{HomeScreen(s.settings.record(s.difficulty),{},{},{});DifficultySheet(vm::select){vm.navigate(Screen.HOME)}}
                Screen.PRACTICE->PracticeScreen(s.practiceBpm,s.practiceStage,vm::practiceBpm,vm::practiceStage,{vm.newRun(true)}){vm.navigate(Screen.HOME)}
                Screen.COUNTDOWN->CountdownScreen(s.countdown)
                Screen.GAME->Box(Modifier.fillMaxSize().statusBarsPadding().padding(top=8.dp,end=12.dp),contentAlignment=Alignment.TopEnd){
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(Surface.copy(alpha=.6f))
                        .clickable(onClick=vm::pause).semantics{contentDescription=vm.getApplication<android.app.Application>().getString(R.string.pause_action)},contentAlignment=Alignment.Center){Label("Ⅱ",24)}
                }
                Screen.PAUSE->PauseScreen(vm::resume,{vm.newRun()}){vm.navigate(Screen.HOME)}
                Screen.OVER->OverScreen(vm.game,s.newRecord,{vm.newRun()},{vm.navigate(Screen.HOME)},s.settings.reduce)
                Screen.TUTORIAL->TutorialScreen(s.tutorialStep,vm::tutorialNext,vm::tutorialDone)
                Screen.CALIBRATION->CalibrationScreen(s,vm.audio::time,vm::calibrationTap,vm::adjust,vm::saveCalibration,vm::repeatCalibration,vm::skipCalibration)
                Screen.SETTINGS->SettingsScreen(s.settings,vm::music,vm::effects,{vm.toggle("vibration",it)},{vm.toggle("reduce",it)},
                    {vm.calibrate()},{vm.navigate(Screen.CLEAR)},{vm.navigate(Screen.ABOUT)}){vm.navigate(Screen.HOME)}
                Screen.ABOUT->SimpleScreen(stringResource(R.string.about),stringResource(R.string.about_body)){vm.navigate(Screen.SETTINGS)}
                Screen.CLEAR->ConfirmScreen(vm::clearRecords){vm.navigate(Screen.SETTINGS)}
            }
            if(s.screen==Screen.GAME) {
                val judgment=s.judgment?.let{stringResource(when(it){Judgment.PERFECT->R.string.perfect;Judgment.GREAT->R.string.great;Judgment.GOOD->R.string.good;Judgment.EARLY->R.string.early;Judgment.MISS->R.string.miss})}.orEmpty()
                val score=stringResource(R.string.score)+": "+s.points
                val lives=stringResource(R.string.lives,s.lives)
                Box(Modifier.semantics{liveRegion=LiveRegionMode.Polite;contentDescription="$judgment. $score. $lives"})
            }
            if(!s.loaded) Box(Modifier.fillMaxSize().background(Sky),contentAlignment=Alignment.Center){Label(stringResource(R.string.loading))}
        }
    }}
}

@Composable fun HomeScreen(record:Int,play:()->Unit,practice:()->Unit,settings:()->Unit) {
    var entered by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){entered=true}
    val offset by animateDpAsState(if(entered)0.dp else (-40).dp,spring(dampingRatio=.7f),label="title")
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
    val sceneGap=(maxHeight-408.dp).coerceAtLeast(80.dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().offset(y=offset)) {
            val title=stringResource(R.string.app_name)
            val measurer=androidx.compose.ui.text.rememberTextMeasurer()
            val measured=measurer.measure(androidx.compose.ui.text.AnnotatedString(title),style=androidx.compose.ui.text.TextStyle(fontFamily=Rounded,fontWeight=FontWeight.Black,fontSize=56.sp)).size.width
            val widthPx=with(androidx.compose.ui.platform.LocalDensity.current){maxWidth.toPx()}
            val titleSize=(56*(widthPx/(measured+8)).coerceAtMost(1f)).toInt()
            Box(Modifier.align(Alignment.Center)) {
                for(dx in listOf(-2,2)) for(dy in listOf(-2,2)) Label(title,titleSize,Color(0xFFFF7A59),Modifier.offset(dx.dp,dy.dp))
                Label(title,titleSize)
            }
        }
        Label(stringResource(R.string.tagline),13,Muted)
        Spacer(Modifier.height(12.dp));Label(stringResource(R.string.record,number(record)),16,Gold)
        Spacer(Modifier.height(sceneGap))
        Column(Modifier.widthIn(max=400.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Entrance(0){Chunky(stringResource(R.string.play),onClick=play)}
            Entrance(80){Chunky(stringResource(R.string.practice),1,onClick=practice)}
            Entrance(160){Chunky(stringResource(R.string.settings),2,onClick=settings)}
        }
    }
}
}
@Composable fun DifficultySheet(select:(Difficulty)->Unit,back:()->Unit) {
    Box(Modifier.fillMaxSize().background(Sky.copy(alpha=.65f)),contentAlignment=Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp)).background(Surface).navigationBarsPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Label(stringResource(R.string.difficulty),24)
            Difficulty.entries.forEachIndexed{i,d->
                Row(Modifier.fillMaxWidth().heightIn(min=72.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceHigh).clickable{select(d)}.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)){Label(difficultyName(d),24);Label(stringResource(R.string.bpm_cap,d.cap.toInt()),13,Muted)}
                    Label("★".repeat(i+1),18,Gold)
                }
            }
            Chunky(stringResource(R.string.back),2,onClick=back)
        }
    }
}
@Composable fun PracticeScreen(bpm:Int,stage:Int,onBpm:(Int)->Unit,onStage:(Int)->Unit,start:()->Unit,back:()->Unit) {
    Page(stringResource(R.string.practice),back) {
        Label(stringResource(R.string.practice_hint),16,Muted)
        Label(stringResource(R.string.bpm,bpm),36,Accent)
        VolumeRow(stringResource(R.string.music),(bpm-70)/110f){onBpm((70+it*110).toInt())}
        Label(stringResource(R.string.practice_stage),16)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (1..4).forEach{i->Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(if(i==stage)Accent else SurfaceHigh).clickable{onStage(i)},contentAlignment=Alignment.Center){Label(i.toString(),24,if(i==stage)Sky else Color.White)}}
        }
        Chunky(stringResource(R.string.start),onClick=start)
    }
}
@Composable fun CountdownScreen(count:Int) {
    var target by remember{mutableFloatStateOf(1.4f)}
    LaunchedEffect(count){target=1.4f;kotlinx.coroutines.delay(16);target=1f}
    val scale by animateFloatAsState(target,tween(250),label="countdown")
    Box(Modifier.fillMaxSize().background(Sky.copy(alpha=.25f)),contentAlignment=Alignment.Center){
        Label(if(count==0)stringResource(R.string.go) else count.toString(),96,Gold,Modifier.scale(scale))
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=80.dp).size(120.dp).border(4.dp,Color.White,RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
            Box(Modifier.size(72.dp).background(Accent,RoundedCornerShape(50)))
        }
    }
}
@Composable fun PauseScreen(resume:()->Unit,restart:()->Unit,exit:()->Unit) {
    Overlay(.85f) {Label(stringResource(R.string.pause),24);Chunky(stringResource(R.string.resume),onClick=resume);Chunky(stringResource(R.string.restart),1,onClick=restart);Chunky(stringResource(R.string.exit),2,onClick=exit)}
}
@Composable fun OverScreen(game:Game?,record:Boolean,again:()->Unit,menu:()->Unit,reduce:Boolean=false) {
    val score=game?.score
    var target by remember{mutableIntStateOf(0)}
    LaunchedEffect(score){target=score?.points?:0}
    val animated by animateIntAsState(target,tween(800),label="final score")
    Box(Modifier.fillMaxSize()) {
    if(record && !reduce) Confetti()
    Overlay(.6f) {
        Label(stringResource(R.string.game_over),24)
        Label(number(animated),56,Gold)
        if(record) Label(stringResource(R.string.new_record),16,Sky,Modifier.background(Gold,RoundedCornerShape(50)).padding(8.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)){Stat(stringResource(R.string.best_combo),(score?.bestCombo?:0).toString());Stat(stringResource(R.string.stage_reached),(game?.tempo?.stageAt(game.time)?:1).toString())}
            Column(Modifier.weight(1f)){Stat(stringResource(R.string.perfects),(score?.perfects?:0).toString());Stat(stringResource(R.string.accuracy),"${(score?.accuracy?:0.0).toInt()} %")}
        }
        Chunky(stringResource(R.string.again),onClick=again);Chunky(stringResource(R.string.menu),2,onClick=menu)
    }
    }
}
@Composable private fun Stat(title:String,value:String) {Label(title,13,Muted);Label(value,24);Spacer(Modifier.height(8.dp))}
@Composable fun TutorialScreen(step:Int,next:()->Unit,skip:()->Unit) {
    Overlay(.7f) {
        Label(stringResource(when(step){0->R.string.tutorial_1;1->R.string.tutorial_2;else->R.string.tutorial_3}),16)
        if(step==1) {
            val description=stringResource(R.string.tutorial_circle)
            Box(Modifier.fillMaxWidth().height(140.dp),contentAlignment=Alignment.Center) {
                var target by remember{mutableFloatStateOf(108f)}
                LaunchedEffect(Unit){target=36f}
                val radius by animateFloatAsState(target,tween(1100,easing=LinearEasing),label="tutorial ring")
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White,radius.dp.toPx(),style=androidx.compose.ui.graphics.drawscope.Stroke(6.dp.toPx()))
                }
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(50)).background(Accent).clickable(onClick=next).semantics{contentDescription=description})
            }
        } else Chunky(stringResource(R.string.next),onClick=next)
        Chunky(stringResource(R.string.skip),2,onClick=skip)
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun CalibrationScreen(s:MenuState,clock:()->Double,tap:(Long)->Unit,adjust:(Int)->Unit,save:()->Unit,repeat:()->Unit,skip:()->Unit) {
    Page(stringResource(R.string.calibration),skip) {
        Label(stringResource(R.string.calibration_hint),16,Muted)
        val description=stringResource(R.string.tap_calibration)
        val subtractDescription=stringResource(R.string.subtract_ms)
        val addDescription=stringResource(R.string.add_ms)
        var frame by remember{mutableLongStateOf(0)}
        LaunchedEffect(s.calibrationCount){if(s.calibrationCount<8)while(true){withFrameNanos{frame=it}}}
        val pulse=remember{derivedStateOf{frame;1f+(.6-(clock()%.6)).toFloat()*.1f}}
        Box(Modifier.fillMaxWidth().height(160.dp),contentAlignment=Alignment.Center) {
            Box(Modifier.size(120.dp).scale(if(s.settings.reduce)1f else pulse.value).clip(RoundedCornerShape(50)).background(Accent)
                .semantics{contentDescription=description;onClick{tap(android.os.SystemClock.uptimeMillis());true}}
                .pointerInteropFilter{e->if(e.actionMasked==MotionEvent.ACTION_DOWN)tap(e.eventTime);true})
        }
        Label(stringResource(R.string.calibration_count,s.calibrationCount),24,Accent)
        Label(stringResource(R.string.adjustment,s.adjustment),24)
        Label(stringResource(R.string.manual),13,Muted)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Chunky("−5 ms",2,Modifier.weight(1f).semantics{contentDescription=subtractDescription}){adjust(-5)}
            Chunky("+5 ms",2,Modifier.weight(1f).semantics{contentDescription=addDescription}){adjust(5)}
        }
        Chunky(stringResource(R.string.save),onClick=save)
        Chunky(stringResource(R.string.repeat),1,onClick=repeat)
        Label(stringResource(R.string.calibration_retry_hint),13,Muted)
    }
}
@Composable fun SettingsScreen(s:Settings,music:(Float)->Unit,effects:(Float)->Unit,vibration:(Boolean)->Unit,reduce:(Boolean)->Unit,calibrate:()->Unit,clear:()->Unit,about:()->Unit,back:()->Unit) {
    Page(stringResource(R.string.settings),back) {
        Panel {
            VolumeRow(stringResource(R.string.music),s.music,music)
            VolumeRow(stringResource(R.string.effects),s.effects,effects)
            ToggleRow(stringResource(R.string.vibration),s.vibration,vibration)
            ToggleRow(stringResource(R.string.reduce),s.reduce,reduce)
            SettingLink(stringResource(R.string.calibration),calibrate)
            Label(stringResource(R.string.adjustment,s.calibration),13,Muted)
            SettingLink(stringResource(R.string.clear_records),clear)
            SettingLink(stringResource(R.string.about),about)
        }
    }
}
@Composable private fun SettingLink(text:String,click:()->Unit) {Box(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable(onClick=click).padding(12.dp),contentAlignment=Alignment.CenterStart){Label(text,16)}}
@Composable fun SimpleScreen(title:String,body:String,back:()->Unit) {Page(title,back){Label(body,16,Muted)}}
@Composable fun ConfirmScreen(clear:()->Unit,cancel:()->Unit) {Overlay(.85f){Label(stringResource(R.string.clear_confirm),24);Chunky(stringResource(R.string.delete),onClick=clear);Chunky(stringResource(R.string.cancel),2,onClick=cancel)}}
@Composable private fun Page(title:String,back:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().background(Sky).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Label(title,24);content();Chunky(stringResource(R.string.back),2,onClick=back)
    }
}
@Composable private fun Overlay(alpha:Float,content:@Composable ColumnScope.()->Unit) {
    Box(Modifier.fillMaxSize().background(Sky.copy(alpha=alpha)).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),contentAlignment=Alignment.Center) {Panel(content=content)}
}

@Preview(widthDp=360,heightDp=800) @Composable private fun HomePreview(){HomeScreen(18200,{},{},{})}
@Preview(widthDp=320,heightDp=640,fontScale=1.3f) @Composable private fun DifficultyPreview(){DifficultySheet({},{})}
@Preview(widthDp=411,heightDp=914) @Composable private fun PracticePreview(){PracticeScreen(100,3,{},{},{},{})}
@Preview @Composable private fun CountdownPreview(){CountdownScreen(3)}
@Preview @Composable private fun PausePreview(){PauseScreen({},{},{})}
@Preview @Composable private fun OverPreview(){OverScreen(null,true,{},{})}
@Preview @Composable private fun TutorialPreview(){TutorialScreen(1,{},{})}
@Preview @Composable private fun CalibrationPreview(){CalibrationScreen(MenuState(),{0.0},{},{},{},{},{})}
@Preview(widthDp=320,heightDp=640,fontScale=1.3f) @Composable private fun SettingsPreview(){SettingsScreen(Settings(),{},{},{},{},{},{},{},{})}
@Preview @Composable private fun AboutPreview(){SimpleScreen("Saltabeat","1.0.0 · MIT",{})}

@Composable private fun Confetti() {
    var target by remember{mutableFloatStateOf(0f)}
    LaunchedEffect(Unit){target=1f}
    val progress by animateFloatAsState(target,tween(1200,easing=LinearEasing),label="confetti")
    val colors=remember{arrayOf(Gold,Accent,Color(0xFFB58CFF),Color(0xFFFF7A59))}
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        if(progress<1f)for(i in 0 until 36) {
            val x=((i*73)%360)/360f*size.width+kotlin.math.sin(progress*8+i)*20.dp.toPx()
            val y=progress*size.height*(.5f+(i%7)/10f)-i%5*24.dp.toPx()
            drawCircle(colors[i%4].copy(alpha=1-progress),3.dp.toPx(),androidx.compose.ui.geometry.Offset(x,y))
        }
    }
}
