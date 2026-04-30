package com.mantismoonlabs.fujinetgo800.input

object AtariKeyCode {
    const val AKEY_SHFT = 0x40
    const val AKEY_CTRL = 0x80

    const val AKEY_0 = 0x32
    const val AKEY_1 = 0x1f
    const val AKEY_2 = 0x1e
    const val AKEY_3 = 0x1a
    const val AKEY_4 = 0x18
    const val AKEY_5 = 0x1d
    const val AKEY_6 = 0x1b
    const val AKEY_7 = 0x33
    const val AKEY_8 = 0x35
    const val AKEY_9 = 0x30

    const val AKEY_a = 0x3f
    const val AKEY_b = 0x15
    const val AKEY_c = 0x12
    const val AKEY_d = 0x3a
    const val AKEY_e = 0x2a
    const val AKEY_f = 0x38
    const val AKEY_g = 0x3d
    const val AKEY_h = 0x39
    const val AKEY_i = 0x0d
    const val AKEY_j = 0x01
    const val AKEY_k = 0x05
    const val AKEY_l = 0x00
    const val AKEY_m = 0x25
    const val AKEY_n = 0x23
    const val AKEY_o = 0x08
    const val AKEY_p = 0x0a
    const val AKEY_q = 0x2f
    const val AKEY_r = 0x28
    const val AKEY_s = 0x3e
    const val AKEY_t = 0x2d
    const val AKEY_u = 0x0b
    const val AKEY_v = 0x10
    const val AKEY_w = 0x2e
    const val AKEY_x = 0x16
    const val AKEY_y = 0x2b
    const val AKEY_z = 0x17

    const val AKEY_A = AKEY_SHFT or AKEY_a
    const val AKEY_CTRL_a = AKEY_CTRL or AKEY_a

    const val AKEY_BACKSPACE = 0x34
    const val AKEY_TAB = 0x2c
    const val AKEY_RETURN = 0x0c
    const val AKEY_SPACE = 0x21
    const val AKEY_ESCAPE = 0x1c
    const val AKEY_BREAK = -5
    const val AKEY_F1 = 0x03
    const val AKEY_HELP = 0x11
    const val AKEY_UP = 0x8e
    const val AKEY_RIGHT = 0x87
    const val AKEY_DOWN = 0x8f
    const val AKEY_LEFT = 0x86
    const val AKEY_DELETE_CHAR = 0xb4
    const val AKEY_DELETE_LINE = 0x74
    const val AKEY_INSERT_CHAR = 0xb7
    const val AKEY_CLEAR = AKEY_SHFT or 0x36
    const val AKEY_MINUS = 0x0e
    const val AKEY_EQUAL = 0x0f
    const val AKEY_SLASH = 0x26
    const val AKEY_COMMA = 0x20
    const val AKEY_FULLSTOP = 0x22
    const val AKEY_SEMICOLON = 0x02
    const val AKEY_QUOTE = 0x73
    const val AKEY_BRACKETLEFT = 0x60
    const val AKEY_BRACKETRIGHT = 0x62
    const val AKEY_BACKSLASH = 0x46
    const val AKEY_CIRCUMFLEX = 0x47
    const val AKEY_PLUS = 0x06
    const val AKEY_ASTERISK = 0x07
    const val AKEY_QUESTION = 0x66
    const val AKEY_LESS = 0x36
    const val AKEY_GREATER = 0x37
    const val AKEY_COLON = 0x42
    const val AKEY_DBLQUOTE = 0x5e
    const val AKEY_UNDERSCORE = 0x4e
    const val AKEY_EXCLAMATION = 0x5f
    const val AKEY_AT = 0x75
    const val AKEY_HASH = 0x5a
    const val AKEY_DOLLAR = 0x58
    const val AKEY_PERCENT = 0x5d
    const val AKEY_AMPERSAND = 0x5b
    const val AKEY_PARENLEFT = 0x70
    const val AKEY_PARENRIGHT = 0x72
    const val AKEY_BAR = 0x4f
    const val AKEY_CARET = AKEY_SHFT or AKEY_ASTERISK
    const val AKEY_ATARI = 0x27
    const val AKEY_CAPSTOGGLE = 0x3c
}

enum class AtariConsoleKey {
    START,
    SELECT,
    OPTION,
}

data class AtariKeyMapping(
    val aKeyCode: Int? = null,
    val consoleKey: AtariConsoleKey? = null,
)
