package com.mantismoonlabs.fujinetgo800.input

import android.view.KeyEvent

class AndroidAtariKeyMapper {
    fun mapKeyEvent(event: KeyEvent): AtariKeyMapping? {
        return mapKeyEvent(
            AndroidKeyInput(
                keyCode = event.keyCode,
                shiftPressed = event.isShiftPressed,
                ctrlPressed = event.isCtrlPressed,
            ),
        )
    }

    fun mapKeyEvent(input: AndroidKeyInput): AtariKeyMapping? {
        consoleKeyFor(input.keyCode)?.let { consoleKey ->
            return AtariKeyMapping(consoleKey = consoleKey)
        }

        specialKeyFor(input.keyCode)?.let { specialKey ->
            return AtariKeyMapping(aKeyCode = specialKey)
        }

        alphaNumericKeyFor(input)?.let { aKeyCode ->
            return AtariKeyMapping(aKeyCode = aKeyCode)
        }

        punctuationKeyFor(input)?.let { aKeyCode ->
            return AtariKeyMapping(aKeyCode = aKeyCode)
        }

        return null
    }

    fun mapCharacter(character: Char): AtariKeyMapping? {
        val aKeyCode = when (character) {
            in 'a'..'z' -> lowerCaseLetters.getValue(character)
            in 'A'..'Z' -> upperCaseLetters.getValue(character)
            in '0'..'9' -> digits.getValue(character)
            ' ' -> AtariKeyCode.AKEY_SPACE
            '\n' -> AtariKeyCode.AKEY_RETURN
            '\t' -> AtariKeyCode.AKEY_TAB
            '-' -> AtariKeyCode.AKEY_MINUS
            '=' -> AtariKeyCode.AKEY_EQUAL
            '/' -> AtariKeyCode.AKEY_SLASH
            ',' -> AtariKeyCode.AKEY_COMMA
            '.' -> AtariKeyCode.AKEY_FULLSTOP
            ';' -> AtariKeyCode.AKEY_SEMICOLON
            '\'' -> AtariKeyCode.AKEY_QUOTE
            '[' -> AtariKeyCode.AKEY_BRACKETLEFT
            ']' -> AtariKeyCode.AKEY_BRACKETRIGHT
            '\\' -> AtariKeyCode.AKEY_BACKSLASH
            '_' -> AtariKeyCode.AKEY_UNDERSCORE
            '+' -> AtariKeyCode.AKEY_PLUS
            '?' -> AtariKeyCode.AKEY_QUESTION
            '<' -> AtariKeyCode.AKEY_LESS
            '>' -> AtariKeyCode.AKEY_GREATER
            ':' -> AtariKeyCode.AKEY_COLON
            '"' -> AtariKeyCode.AKEY_DBLQUOTE
            '!' -> AtariKeyCode.AKEY_EXCLAMATION
            '@' -> AtariKeyCode.AKEY_AT
            '#' -> AtariKeyCode.AKEY_HASH
            '$' -> AtariKeyCode.AKEY_DOLLAR
            '%' -> AtariKeyCode.AKEY_PERCENT
            '&' -> AtariKeyCode.AKEY_AMPERSAND
            '*' -> AtariKeyCode.AKEY_ASTERISK
            '(' -> AtariKeyCode.AKEY_PARENLEFT
            ')' -> AtariKeyCode.AKEY_PARENRIGHT
            '|' -> AtariKeyCode.AKEY_BAR
            '^' -> AtariKeyCode.AKEY_CARET
            else -> return null
        }
        return AtariKeyMapping(aKeyCode = aKeyCode)
    }

    private fun consoleKeyFor(keyCode: Int): AtariConsoleKey? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_START -> AtariConsoleKey.START
        KeyEvent.KEYCODE_BUTTON_SELECT -> AtariConsoleKey.SELECT
        KeyEvent.KEYCODE_MENU -> AtariConsoleKey.OPTION
        else -> null
    }

    private fun specialKeyFor(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_CENTER -> AtariKeyCode.AKEY_RETURN
        KeyEvent.KEYCODE_SPACE -> AtariKeyCode.AKEY_SPACE
        KeyEvent.KEYCODE_DEL -> AtariKeyCode.AKEY_BACKSPACE
        KeyEvent.KEYCODE_FORWARD_DEL -> AtariKeyCode.AKEY_DELETE_CHAR
        KeyEvent.KEYCODE_INSERT -> AtariKeyCode.AKEY_INSERT_CHAR
        KeyEvent.KEYCODE_TAB -> AtariKeyCode.AKEY_TAB
        KeyEvent.KEYCODE_ESCAPE -> AtariKeyCode.AKEY_ESCAPE
        KeyEvent.KEYCODE_SYSRQ,
        KeyEvent.KEYCODE_BREAK -> AtariKeyCode.AKEY_BREAK
        KeyEvent.KEYCODE_F1 -> AtariKeyCode.AKEY_F1
        KeyEvent.KEYCODE_MOVE_HOME -> AtariKeyCode.AKEY_CLEAR
        KeyEvent.KEYCODE_DPAD_UP -> AtariKeyCode.AKEY_UP
        KeyEvent.KEYCODE_DPAD_RIGHT -> AtariKeyCode.AKEY_RIGHT
        KeyEvent.KEYCODE_DPAD_DOWN -> AtariKeyCode.AKEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> AtariKeyCode.AKEY_LEFT
        else -> null
    }

    private fun alphaNumericKeyFor(input: AndroidKeyInput): Int? {
        val base = when (input.keyCode) {
            KeyEvent.KEYCODE_A -> AtariKeyCode.AKEY_a
            KeyEvent.KEYCODE_B -> AtariKeyCode.AKEY_b
            KeyEvent.KEYCODE_C -> AtariKeyCode.AKEY_c
            KeyEvent.KEYCODE_D -> AtariKeyCode.AKEY_d
            KeyEvent.KEYCODE_E -> AtariKeyCode.AKEY_e
            KeyEvent.KEYCODE_F -> AtariKeyCode.AKEY_f
            KeyEvent.KEYCODE_G -> AtariKeyCode.AKEY_g
            KeyEvent.KEYCODE_H -> AtariKeyCode.AKEY_h
            KeyEvent.KEYCODE_I -> AtariKeyCode.AKEY_i
            KeyEvent.KEYCODE_J -> AtariKeyCode.AKEY_j
            KeyEvent.KEYCODE_K -> AtariKeyCode.AKEY_k
            KeyEvent.KEYCODE_L -> AtariKeyCode.AKEY_l
            KeyEvent.KEYCODE_M -> AtariKeyCode.AKEY_m
            KeyEvent.KEYCODE_N -> AtariKeyCode.AKEY_n
            KeyEvent.KEYCODE_O -> AtariKeyCode.AKEY_o
            KeyEvent.KEYCODE_P -> AtariKeyCode.AKEY_p
            KeyEvent.KEYCODE_Q -> AtariKeyCode.AKEY_q
            KeyEvent.KEYCODE_R -> AtariKeyCode.AKEY_r
            KeyEvent.KEYCODE_S -> AtariKeyCode.AKEY_s
            KeyEvent.KEYCODE_T -> AtariKeyCode.AKEY_t
            KeyEvent.KEYCODE_U -> AtariKeyCode.AKEY_u
            KeyEvent.KEYCODE_V -> AtariKeyCode.AKEY_v
            KeyEvent.KEYCODE_W -> AtariKeyCode.AKEY_w
            KeyEvent.KEYCODE_X -> AtariKeyCode.AKEY_x
            KeyEvent.KEYCODE_Y -> AtariKeyCode.AKEY_y
            KeyEvent.KEYCODE_Z -> AtariKeyCode.AKEY_z
            KeyEvent.KEYCODE_0 -> AtariKeyCode.AKEY_0
            KeyEvent.KEYCODE_1 -> AtariKeyCode.AKEY_1
            KeyEvent.KEYCODE_2 -> AtariKeyCode.AKEY_2
            KeyEvent.KEYCODE_3 -> AtariKeyCode.AKEY_3
            KeyEvent.KEYCODE_4 -> AtariKeyCode.AKEY_4
            KeyEvent.KEYCODE_5 -> AtariKeyCode.AKEY_5
            KeyEvent.KEYCODE_6 -> AtariKeyCode.AKEY_6
            KeyEvent.KEYCODE_7 -> AtariKeyCode.AKEY_7
            KeyEvent.KEYCODE_8 -> AtariKeyCode.AKEY_8
            KeyEvent.KEYCODE_9 -> AtariKeyCode.AKEY_9
            else -> return null
        }

        if (input.keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            return when {
                input.ctrlPressed && input.shiftPressed -> base or AtariKeyCode.AKEY_CTRL or AtariKeyCode.AKEY_SHFT
                input.ctrlPressed -> base or AtariKeyCode.AKEY_CTRL
                input.shiftPressed -> base or AtariKeyCode.AKEY_SHFT
                else -> base
            }
        }

        if (input.ctrlPressed) {
            return base or AtariKeyCode.AKEY_CTRL
        }

        if (input.shiftPressed) {
            return shiftedDigitMap[input.keyCode] ?: base
        }

        return base
    }

    private fun punctuationKeyFor(input: AndroidKeyInput): Int? {
        val shifted = input.shiftPressed
        return when (input.keyCode) {
            KeyEvent.KEYCODE_MINUS -> if (shifted) AtariKeyCode.AKEY_UNDERSCORE else AtariKeyCode.AKEY_MINUS
            KeyEvent.KEYCODE_EQUALS -> if (shifted) AtariKeyCode.AKEY_PLUS else AtariKeyCode.AKEY_EQUAL
            KeyEvent.KEYCODE_SLASH -> if (shifted) AtariKeyCode.AKEY_QUESTION else AtariKeyCode.AKEY_SLASH
            KeyEvent.KEYCODE_COMMA -> if (shifted) AtariKeyCode.AKEY_LESS else AtariKeyCode.AKEY_COMMA
            KeyEvent.KEYCODE_PERIOD -> if (shifted) AtariKeyCode.AKEY_GREATER else AtariKeyCode.AKEY_FULLSTOP
            KeyEvent.KEYCODE_SEMICOLON -> if (shifted) AtariKeyCode.AKEY_COLON else AtariKeyCode.AKEY_SEMICOLON
            KeyEvent.KEYCODE_APOSTROPHE -> if (shifted) AtariKeyCode.AKEY_DBLQUOTE else AtariKeyCode.AKEY_QUOTE
            KeyEvent.KEYCODE_LEFT_BRACKET -> if (shifted) AtariKeyCode.AKEY_ATARI else AtariKeyCode.AKEY_BRACKETLEFT
            KeyEvent.KEYCODE_RIGHT_BRACKET -> AtariKeyCode.AKEY_BRACKETRIGHT
            KeyEvent.KEYCODE_BACKSLASH -> AtariKeyCode.AKEY_BACKSLASH
            KeyEvent.KEYCODE_GRAVE -> AtariKeyCode.AKEY_CAPSTOGGLE
            else -> null
        }
    }

    private companion object {
        val lowerCaseLetters = mapOf(
            'a' to AtariKeyCode.AKEY_a,
            'b' to AtariKeyCode.AKEY_b,
            'c' to AtariKeyCode.AKEY_c,
            'd' to AtariKeyCode.AKEY_d,
            'e' to AtariKeyCode.AKEY_e,
            'f' to AtariKeyCode.AKEY_f,
            'g' to AtariKeyCode.AKEY_g,
            'h' to AtariKeyCode.AKEY_h,
            'i' to AtariKeyCode.AKEY_i,
            'j' to AtariKeyCode.AKEY_j,
            'k' to AtariKeyCode.AKEY_k,
            'l' to AtariKeyCode.AKEY_l,
            'm' to AtariKeyCode.AKEY_m,
            'n' to AtariKeyCode.AKEY_n,
            'o' to AtariKeyCode.AKEY_o,
            'p' to AtariKeyCode.AKEY_p,
            'q' to AtariKeyCode.AKEY_q,
            'r' to AtariKeyCode.AKEY_r,
            's' to AtariKeyCode.AKEY_s,
            't' to AtariKeyCode.AKEY_t,
            'u' to AtariKeyCode.AKEY_u,
            'v' to AtariKeyCode.AKEY_v,
            'w' to AtariKeyCode.AKEY_w,
            'x' to AtariKeyCode.AKEY_x,
            'y' to AtariKeyCode.AKEY_y,
            'z' to AtariKeyCode.AKEY_z,
        )
        val upperCaseLetters = mapOf(
            'A' to AtariKeyCode.AKEY_A,
            'B' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_b),
            'C' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_c),
            'D' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_d),
            'E' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_e),
            'F' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_f),
            'G' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_g),
            'H' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_h),
            'I' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_i),
            'J' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_j),
            'K' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_k),
            'L' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_l),
            'M' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_m),
            'N' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_n),
            'O' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_o),
            'P' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_p),
            'Q' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_q),
            'R' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_r),
            'S' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_s),
            'T' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_t),
            'U' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_u),
            'V' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_v),
            'W' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_w),
            'X' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_x),
            'Y' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_y),
            'Z' to (AtariKeyCode.AKEY_SHFT or AtariKeyCode.AKEY_z),
        )
        val digits = mapOf(
            '0' to AtariKeyCode.AKEY_0,
            '1' to AtariKeyCode.AKEY_1,
            '2' to AtariKeyCode.AKEY_2,
            '3' to AtariKeyCode.AKEY_3,
            '4' to AtariKeyCode.AKEY_4,
            '5' to AtariKeyCode.AKEY_5,
            '6' to AtariKeyCode.AKEY_6,
            '7' to AtariKeyCode.AKEY_7,
            '8' to AtariKeyCode.AKEY_8,
            '9' to AtariKeyCode.AKEY_9,
        )
        val shiftedDigitMap = mapOf(
            KeyEvent.KEYCODE_1 to AtariKeyCode.AKEY_EXCLAMATION,
            KeyEvent.KEYCODE_2 to AtariKeyCode.AKEY_AT,
            KeyEvent.KEYCODE_3 to AtariKeyCode.AKEY_HASH,
            KeyEvent.KEYCODE_4 to AtariKeyCode.AKEY_DOLLAR,
            KeyEvent.KEYCODE_5 to AtariKeyCode.AKEY_PERCENT,
            KeyEvent.KEYCODE_6 to AtariKeyCode.AKEY_CARET,
            KeyEvent.KEYCODE_7 to AtariKeyCode.AKEY_AMPERSAND,
            KeyEvent.KEYCODE_8 to AtariKeyCode.AKEY_ASTERISK,
            KeyEvent.KEYCODE_9 to AtariKeyCode.AKEY_PARENLEFT,
            KeyEvent.KEYCODE_0 to AtariKeyCode.AKEY_PARENRIGHT,
        )
    }
}

data class AndroidKeyInput(
    val keyCode: Int,
    val shiftPressed: Boolean = false,
    val ctrlPressed: Boolean = false,
)
