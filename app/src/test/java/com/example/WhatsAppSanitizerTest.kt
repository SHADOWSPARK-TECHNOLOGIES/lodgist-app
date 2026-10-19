package com.example

import com.example.ui.components.sanitizeForWhatsApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppSanitizerTest {

  private fun assertNoneOf(clean: String, vararg chars: Char) =
    chars.forEach { assertFalse("U+%04X must be removed".format(it.code), clean.contains(it)) }

  @Test
  fun removesLineAndParagraphSeparators() {
    val clean = sanitizeForWhatsApp("Flat\u2028Pay here\u2029Now")
    assertNoneOf(clean, '\u2028', '\u2029')
    assertEquals("Flat Pay here Now", clean)
  }

  @Test
  fun removesBidiOverrideAndIsolates() {
    val clean = sanitizeForWhatsApp("Lekki\u202E321\u2066Ikoyi\u2069")
    assertNoneOf(clean, '\u202E', '\u2066', '\u2069')
    assertEquals("Lekki 321 Ikoyi", clean)
  }

  @Test
  fun removesControlChars() {
    val clean = sanitizeForWhatsApp("a\u0000b\u0007c\u001Bd\u007Fe\u0085f\r\ng")
    assertEquals("a b c d e f g", clean)
  }

  @Test
  fun capsAt80CodePointsWithoutSplittingSurrogatePairs() {
    val plain = sanitizeForWhatsApp("x".repeat(200))
    assertEquals(80, plain.length)
    assertTrue(plain.endsWith("…"))

    val emoji = "\uD83C\uDFE0" // house emoji, one code point, two chars
    val clean = sanitizeForWhatsApp(emoji.repeat(100))
    assertEquals(80, clean.codePointCount(0, clean.length))
    clean.dropLast(1).chunked(2).forEach { assertEquals(emoji, it) }
    assertEquals(emoji.repeat(10), sanitizeForWhatsApp(emoji.repeat(10)))
  }
}
