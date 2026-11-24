package persona.core.designsystem

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import persona.core.domain.models.PanelConfig
import persona.core.domain.models.PanelModel
import persona.core.domain.models.PanelPrefs
import persona.core.domain.models.PanelProvider
import persona.core.domain.models.PanelState

/**
 * A launcher whose list names a model twice opens the panel (#9998): Claude Code lists `default` twice when its
 * settings say `"model": "default"`, and two rows with one key made the list throw as it opened, closing the app.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "en-w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModelPanelRepeatedModelTest {
    @get:Rule val rule = createComposeRule()

    private val words = ModelPanelWords(reason = { "" }, effort = { it }, engine = { it }, note = { "" }, mark = { null })

    @Test fun aModelListedTwiceOpensWithOneRow() {
        val default = PanelModel("default", "Default (recommended)", resolved = "claude-opus-5-5", providerDefault = true)
        val claude = PanelProvider("claude", "claude", "claude",
            listOf(default, PanelModel("opus", "Opus"), PanelModel("sonnet", "Sonnet"), default.copy(name = "Default")))
        val state = PanelState(current = PanelConfig("claude", "default"), recommended = listOf("claude"))
        rule.setContent {
            PersonaTheme(darkTheme = false, reduceMotion = true) {
                JModelPanel(listOf(claude, claude), state, PanelPrefs(), {}, words, onPick = {}, onDismiss = {})
            }
        }
        rule.onNodeWithTag("model-panel-list").assertExists()
        assertEquals(1, rule.onAllNodesWithText("Default (recommended)").fetchSemanticsNodes().size)
    }

    @Test fun changingEffortAndResettingKeepTheSliderInPlace() {
        val model = PanelModel("example-model", "Example model", efforts = listOf("low", "medium", "high"), defaultEffort = "low")
        val provider = PanelProvider("example", "Example", "cursor", listOf(model))
        var prefs by mutableStateOf(PanelPrefs())
        var dark by mutableStateOf(false)
        rule.setContent {
            PersonaTheme(darkTheme = dark, reduceMotion = true) {
                JModelPanel(listOf(provider), PanelState(recommended = listOf(provider.id)), prefs, { prefs = it }, words,
                    onPick = {}, onDismiss = {})
            }
        }
        rule.onNodeWithContentDescription("Customize").performClick()
        val slider = rule.onNodeWithContentDescription("Thinking effort")
        val before = slider.getBoundsInRoot()

        slider.performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(1f)) }
        val reset = rule.onNodeWithText("Reset to recommended").assertExists()
        assertEquals(before, slider.getBoundsInRoot())

        val output = File("build/model-effort-sheet-shots").also { it.mkdirs() }
        for (darkTheme in listOf(false, true)) {
            dark = darkTheme
            rule.waitForIdle()
            val bitmap = rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
            File(output, if (darkTheme) "dark.png" else "light.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        reset.performClick()
        reset.assertDoesNotExist()
        assertEquals(before, slider.getBoundsInRoot())
    }

}
