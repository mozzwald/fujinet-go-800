package com.mantismoonlabs.fujinetgo800.ui.input

import com.mantismoonlabs.fujinetgo800.input.AtariKeyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtariKeyboardTest {
    @Test
    fun regularLayerUsesLowercaseLegends() {
        assertEquals(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            keyboardRows()[1].map { it.displayLabel(fnEnabled = false, shiftEnabled = false) },
        )
    }

    @Test
    fun shiftLayerUsesAtari800xlNumberLegends() {
        assertEquals(
            listOf("!", "\"", "#", "$", "%", "&", "'", "@", "(", ")"),
            keyboardRows()[0].map { it.displayLabel(fnEnabled = false, shiftEnabled = true) },
        )

        val shiftedSix = keyboardRows()[0][5].resolve(shiftEnabled = true)

        assertEquals(listOf(AtariKeyCode.AKEY_AMPERSAND), shiftedSix.pressAkeyCodes())
    }

    @Test
    fun symLayerExposesSpecialKeys() {
        val symLabels = keyboardRows().map { row ->
            row.map { it.displayLabel(fnEnabled = true, shiftEnabled = false) }
        }

        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "CLR", "INS", "BRK"), symLabels[0])
        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "[", "]", "CAPS"), symLabels[1])
        assertEquals(listOf("TAB", "a", "s", "d", "f", ";", "_", "-", "|", "="), symLabels[2])
        assertEquals(listOf("CTRL", "z", "x", ":", "\\", "+", "^", "*", "⌫"), symLabels[3])
        assertEquals(listOf("SHIFT", "SYM", "<", "▁", ">", "↵"), symLabels[4])
    }

    @Test
    fun symSpecialKeysDispatchExpectedAtariCodes() {
        assertEquals(AtariKeyCode.AKEY_CLEAR, keyboardRows()[0][7].resolve(fnEnabled = true).singlePressAkeyCode())
        assertEquals(AtariKeyCode.AKEY_INSERT_CHAR, keyboardRows()[0][8].resolve(fnEnabled = true).singlePressAkeyCode())
        assertEquals(AtariKeyCode.AKEY_BREAK, keyboardRows()[0][9].resolve(fnEnabled = true).singlePressAkeyCode())
        assertEquals(AtariKeyCode.AKEY_CAPSTOGGLE, keyboardRows()[1][9].resolve(fnEnabled = true).singlePressAkeyCode())
        assertEquals(AtariKeyCode.AKEY_CIRCUMFLEX, keyboardRows()[3][6].resolve(fnEnabled = true).singlePressAkeyCode())
    }

    @Test
    fun inverseEligibilityFollowsActiveLayer() {
        val zeroKey = keyboardRows()[0][9]
        val pKey = keyboardRows()[1][9]

        assertTrue(zeroKey.isInverseEligible(fnEnabled = false))
        assertTrue(pKey.isInverseEligible(fnEnabled = false))
        assertFalse(zeroKey.isInverseEligible(fnEnabled = true))
        assertFalse(pKey.isInverseEligible(fnEnabled = true))
    }

    @Test
    fun ctrlKeepsLegendsAndAppliesModifier() {
        val cKey = keyboardRows()[3][3]

        assertEquals("c", cKey.displayLabel(fnEnabled = false, shiftEnabled = false))

        val resolved = cKey.resolve(ctrlEnabled = true)

        assertEquals(listOf(AtariKeyCode.AKEY_CTRL or AtariKeyCode.AKEY_c), resolved.pressAkeyCodes())
        assertTrue(resolved.clearsCtrl)
        assertFalse(resolved.clearsShift)
        assertFalse(resolved.clearsFn)
    }

    @Test
    fun backspaceShowsDeleteLabelsForShiftAndCtrl() {
        val backspaceKey = keyboardRows()[3][8]

        assertEquals("DEL-L", backspaceKey.displayLabel(fnEnabled = false, shiftEnabled = true))
        assertEquals("DEL", backspaceKey.displayLabel(fnEnabled = false, shiftEnabled = false, ctrlEnabled = true))
        assertEquals(AtariKeyCode.AKEY_DELETE_LINE, backspaceKey.resolve(shiftEnabled = true).singlePressAkeyCode())
        assertEquals(AtariKeyCode.AKEY_DELETE_CHAR, backspaceKey.resolve(ctrlEnabled = true).singlePressAkeyCode())
    }

    private fun KeyboardKeySpec.resolve(
        fnEnabled: Boolean = false,
        shiftEnabled: Boolean = false,
        ctrlEnabled: Boolean = false,
    ): ResolvedKeyboardDispatch = resolveKeyDispatch(
        key = this,
        fnEnabled = fnEnabled,
        shiftEnabled = shiftEnabled,
        ctrlEnabled = ctrlEnabled,
    )

    private fun ResolvedKeyboardDispatch.pressAkeyCodes(): List<Int> = pressMappings.mapNotNull { it.aKeyCode }

    private fun ResolvedKeyboardDispatch.singlePressAkeyCode(): Int = pressAkeyCodes().single()
}
