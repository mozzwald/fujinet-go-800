package com.mantismoonlabs.fujinetgo800.ui.input

import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtariKeyboardTest {
    @Test
    fun fnBackspaceDisplaysBreakAndDispatchesBreakKey() {
        val backspaceKey = keyboardRows()
            .flatten()
            .first { it.label == "⌫" }

        assertEquals("BRK", backspaceKey.displayLabel(fnEnabled = true, shiftEnabled = false))
        assertEquals(0.82f, backspaceKey.displayFontScale(fnEnabled = true, shiftEnabled = false))

        val resolved = resolveKeyDispatch(
            key = backspaceKey,
            fnEnabled = true,
            shiftEnabled = false,
            ctrlEnabled = false,
            atariEnabled = false,
        )

        assertEquals(
            listOf(AtariKeyCode.AKEY_BREAK),
            resolved.pressMappings.mapNotNull { it.aKeyCode },
        )
        assertEquals(
            listOf(AtariKeyCode.AKEY_BREAK),
            resolved.releaseMappings.mapNotNull { it.aKeyCode },
        )
        assertTrue(resolved.clearsFn)
        assertFalse(resolved.clearsShift)
        assertFalse(resolved.clearsCtrl)
    }

    @Test
    fun fnEnterDisplaysClearAndDispatchesClearKey() {
        val enterKey = keyboardRows()
            .flatten()
            .first { it.label == "↵" }

        assertEquals("CLEAR", enterKey.displayLabel(fnEnabled = true, shiftEnabled = false))
        assertEquals(0.72f, enterKey.displayFontScale(fnEnabled = true, shiftEnabled = false))

        val resolved = resolveKeyDispatch(
            key = enterKey,
            fnEnabled = true,
            shiftEnabled = false,
            ctrlEnabled = false,
            atariEnabled = false,
        )

        assertEquals(
            listOf(AtariKeyCode.AKEY_CLEAR),
            resolved.pressMappings.mapNotNull { it.aKeyCode },
        )
        assertEquals(
            listOf(AtariKeyCode.AKEY_CLEAR),
            resolved.releaseMappings.mapNotNull { it.aKeyCode },
        )
        assertTrue(resolved.clearsFn)
        assertFalse(resolved.clearsShift)
        assertFalse(resolved.clearsCtrl)
    }
}
