package com.mantismoonlabs.fujinetgo800.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidAtariKeyMapperTest {
    private val mapper = AndroidAtariKeyMapper()

    @Test
    fun mapsRepresentativeTypingKeys() {
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_a),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_A)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_A),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_A, shiftPressed = true)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_CTRL_a),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_A, ctrlPressed = true)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_1),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_1)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_SPACE),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_SPACE)),
        )
    }

    @Test
    fun mapsRepresentativeSpecialKeys() {
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RETURN),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_ENTER)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BACKSPACE),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_DEL)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_BREAK),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_SYSRQ)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_F1),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_F1)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_UP),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_DPAD_UP)),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RIGHT),
            mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_DPAD_RIGHT)),
        )
        assertNull(mapper.mapKeyEvent(AndroidKeyInput(keyCode = KeyEvent.KEYCODE_VOLUME_UP)))
    }

    @Test
    fun mapsRepresentativeImeCharacters() {
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_A),
            mapper.mapCharacter('A'),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_EXCLAMATION),
            mapper.mapCharacter('!'),
        )
        assertEquals(
            AtariKeyMapping(aKeyCode = AtariKeyCode.AKEY_RETURN),
            mapper.mapCharacter('\n'),
        )
        assertNull(mapper.mapCharacter('~'))
    }
}
