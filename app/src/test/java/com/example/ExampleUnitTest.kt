package com.example

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
}
