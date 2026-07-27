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

    @Test
    fun splitRowsPartitionEachRowByHandColumn() {
        val fullRows = keyboardRows()

        // number and qwerty rows: left/right slices reconstruct the exact key sequence with
        // no gaps or overlap.
        for (i in 0..1) {
            assertEquals(fullRows[i], splitLeftRows[i] + splitRightRows[i])
        }

        // Home and control rows: TAB/CTRL are pulled out into splitLeftUtilityKeys (so the
        // remaining letters get more width), so the row reconstructs as [modifier key] +
        // left slice + right slice.
        assertEquals(fullRows[2], listOf(fullRows[2][0]) + splitLeftRows[2] + splitRightRows[2])
        assertEquals(fullRows[3], listOf(fullRows[3][0]) + splitLeftRows[3] + splitRightRows[3])
        assertEquals(listOf(fullRows[2][0], fullRows[3][0]), splitLeftUtilityKeys)

        // Bottom row: everything but the spacebar reconstructs unchanged; the spacebar
        // itself is split into two half-weight keys on either side, both still mapped to
        // AKEY_SPACE.
        val bottomRow = fullRows[4]
        val spaceKey = bottomRow[3]
        assertEquals(bottomRow.take(3), splitLeftRows[4].dropLast(1))
        assertEquals(bottomRow.drop(4), splitRightRows[4].drop(1))

        val leftSpaceHalf = splitLeftRows[4].last()
        val rightSpaceHalf = splitRightRows[4].first()
        assertEquals(spaceKey.mapping, leftSpaceHalf.mapping)
        assertEquals(spaceKey.mapping, rightSpaceHalf.mapping)
        assertEquals(spaceKey.weight, leftSpaceHalf.weight + rightSpaceHalf.weight, 0.0001f)
    }

    @Test
    fun splitRowsCoverHandErgonomicColumnsFromApprovedScheme() {
        assertEquals(listOf("1", "2", "3", "4", "5"), splitLeftRows[0].map { it.label })
        assertEquals(listOf("6", "7", "8", "9", "0"), splitRightRows[0].map { it.label })
        assertEquals(listOf("q", "w", "e", "r", "t"), splitLeftRows[1].map { it.label })
        assertEquals(listOf("y", "u", "i", "o", "p"), splitRightRows[1].map { it.label })
        assertEquals(listOf("a", "s", "d", "f", "g"), splitLeftRows[2].map { it.label })
        assertEquals(listOf("h", "j", "k", "l"), splitRightRows[2].map { it.label })
        assertEquals(listOf("z", "x", "c", "v", "b"), splitLeftRows[3].map { it.label })
        assertEquals(listOf("n", "m", "⌫"), splitRightRows[3].map { it.label })
        assertEquals(listOf("TAB", "CTRL"), splitLeftUtilityKeys.map { it.label })
        assertEquals(listOf("SHIFT", "SYM", "."), splitLeftRows[4].map { it.label }.dropLast(1))
        assertEquals(listOf("/", "↵"), splitRightRows[4].map { it.label }.drop(1))
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
