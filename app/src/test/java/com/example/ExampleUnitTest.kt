package com.example

import com.example.data.local.MemoryDocumentEntity
import com.example.data.model.QuickActionsList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify quick actions list has programming actions`() {
    val actions = QuickActionsList.items
    assertTrue(actions.isNotEmpty())
    assertNotNull(actions.find { it.id == "explain" })
    assertNotNull(actions.find { it.id == "fix_error" })
    assertNotNull(actions.find { it.id == "aml_plugin" })
    assertNotNull(actions.find { it.id == "crash_log" })
  }

  @Test
  fun `verify memory document entity structure`() {
    val doc = MemoryDocumentEntity(
      id = "test-1",
      filename = "Events.h",
      filePath = "aml-psdk/game_sa/Events.h",
      category = "SDK Header",
      content = "class Events { public: static void drawingEvent(); };",
      summary = "class Events",
      sizeBytes = 54,
      isEnabled = true
    )
    assertEquals("Events.h", doc.filename)
    assertTrue(doc.isEnabled)
    assertTrue(doc.content.contains("drawingEvent"))
  }
}
