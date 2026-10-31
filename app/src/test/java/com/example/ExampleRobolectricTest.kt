package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.QuickActionsList
import com.example.data.pref.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Panda AI", appName)
  }

  @Test
  fun `verify app preferences default values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferences(context)
    assertEquals("gemini-2.5-flash", prefs.selectedModel)
    assertTrue(prefs.temperature in 0.0f..1.0f)
    assertTrue(prefs.systemPrompt.contains("AML Mod Loader"))
  }

  @Test
  fun `verify quick actions list has programming actions`() {
    val actions = QuickActionsList.items
    assertTrue(actions.isNotEmpty())
    assertNotNull(actions.find { it.id == "explain" })
    assertNotNull(actions.find { it.id == "fix_error" })
    assertNotNull(actions.find { it.id == "aml_plugin" })
  }
}
