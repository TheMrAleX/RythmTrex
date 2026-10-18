package app.saltabeat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.saltabeat.audio.BeatAudio
import app.saltabeat.data.Settings
import app.saltabeat.engine.*
import app.saltabeat.ui.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreensTest {
    @get:Rule val compose=createComposeRule()
    private fun capture(name:String) {
        val target=File("../docs/screens/$name.png")
        target.parentFile?.mkdirs()
        compose.onRoot().captureRoboImage(target.absolutePath)
        org.junit.Assert.assertTrue("Screenshot must exist: $target",target.isFile && target.length()>0)
    }
    @Test fun homeOpensDifficultyAndSelectionStartsRun() {
        var screen by mutableIntStateOf(0)
        var selected:Difficulty?=null
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Sky)) {
                if(screen==0) HomeScreen(18200,{screen=1},{},{})
                else DifficultySheet({selected=it},{screen=0})
            }
        }
        compose.onNodeWithText("Jugar").performClick()
        compose.onNodeWithText("Difícil").assertIsDisplayed().performClick()
        assert(selected==Difficulty.HARD)
        capture("difficulty-360x800")
    }
    @Test fun homeReference(){compose.setContent{Box(Modifier.fillMaxSize().background(Sky)){GameCanvas(null,remember{BeatAudio()},Settings(),272.dp,false);HomeScreen(18200,{},{},{})}};capture("home-360x800")}
    @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun compactSettingsAreScrollable() {
        compose.setContent{
            val density=androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,1.3f)) {
                SettingsScreen(Settings(),{},{},{},{},{},{},{},{})
            }
        }
        capture("settings-320x640")
        compose.onNodeWithText("Acerca de").performScrollTo().assertIsDisplayed()
    }
    @Test @Config(qualifiers="w411dp-h914dp-mdpi") fun largeGame() {
        val game=Game(41,Difficulty.NORMAL);game.advance(1.5)
        compose.setContent{GameCanvas(game,remember{BeatAudio()},Settings(),310.dp,false)}
        capture("game-411x914")
    }
    @Test fun scaledTextDoesNotHideHomeActions() {
        compose.setContent {
            val current=androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(current.density,1.3f)) {
                Box(Modifier.fillMaxSize().background(Sky)){GameCanvas(null,remember{BeatAudio()},Settings(),272.dp,false);HomeScreen(18200,{},{},{})}
            }
        }
        compose.onNodeWithText("Ajustes").performScrollTo().assertIsDisplayed()
        capture("home-font130")
    }
    @Test fun pauseAndEndActions() {
        var resume=false
        compose.setContent{PauseScreen({resume=true},{},{})}
        compose.onNodeWithText("Seguir").performClick()
        assert(resume)
        capture("pause-360x800")
    }
    @Test fun gameOverStatsAndRetry() {
        var retry=false
        compose.setContent{OverScreen(Game(1,Difficulty.NORMAL),true,{retry=true},{})}
        compose.onNodeWithText("Otra vez").performClick();assert(retry)
        capture("over-360x800")
    }
    @Test fun tutorialRequiresCircleTap() {
        var step=0
        compose.setContent{TutorialScreen(1,{step++},{})}
        compose.onNodeWithContentDescription("Tocar círculo del tutorial").performClick()
        assert(step==1);capture("tutorial-360x800")
    }
    @Test fun calibrationShowsEightAndAdjustment() {
        compose.setContent{CalibrationScreen(MenuState(calibrationCount=8,adjustment=42),{0.0},{},{},{},{},{})}
        compose.onNodeWithText("Tu ajuste: +42 ms").assertIsDisplayed()
        capture("calibration-360x800")
    }
    @Test fun practiceChoosesTempoAndStage() {
        var stage=1
        compose.setContent{PracticeScreen(180,4,{},{stage=it},{},{})}
        compose.onNodeWithText("3").performClick();assert(stage==3)
        capture("practice-360x800")
    }
    @Test fun countdown() {compose.setContent{CountdownScreen(3)};capture("countdown-360x800")}
    @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun compactGameWithLargeText() {
        val game=Game(41,Difficulty.HARD,width=320.0,padHeight=216.0);game.advance(1.1)
        compose.setContent {
            val density=androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,1.3f)) {
                GameCanvas(game,remember{BeatAudio()},Settings(),240.dp,false)
            }
        }
        capture("game-320x640-font130")
    }

}
