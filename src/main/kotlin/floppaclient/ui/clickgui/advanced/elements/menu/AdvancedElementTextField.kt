package floppaclient.ui.clickgui.advanced.elements.menu

import floppaclient.module.Module
import floppaclient.module.settings.impl.StringSetting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.AdvancedElementType
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import org.lwjgl.input.Keyboard
import java.awt.Color

/**
 * Provides a text field element for the advanced gui.
 *
 * @author Aton
 */
class AdvancedElementTextField(
    parent: AdvancedMenu, module: Module, setting: StringSetting,
) : AdvancedElement(parent, module, setting, AdvancedElementType.TEXT_FIELD) {
    private var listening: Boolean = false
/*
    private val keyWhitelist = intArrayOf(
        Keyboard.KEY_W,
        Keyboard.KEY_Q,
        Keyboard.KEY_E,
        Keyboard.KEY_R,
        Keyboard.KEY_T,
        Keyboard.KEY_Z,
        Keyboard.KEY_U,
        Keyboard.KEY_I,
        Keyboard.KEY_O,
        Keyboard.KEY_P,
        Keyboard.KEY_A,
        Keyboard.KEY_S,
        Keyboard.KEY_D,
        Keyboard.KEY_F,
        Keyboard.KEY_G,
        Keyboard.KEY_H,
        Keyboard.KEY_J,
        Keyboard.KEY_K,
        Keyboard.KEY_L,
        Keyboard.KEY_Y,
        Keyboard.KEY_X,
        Keyboard.KEY_C,
        Keyboard.KEY_V,
        Keyboard.KEY_B,
        Keyboard.KEY_N,
        Keyboard.KEY_M,
        Keyboard.KEY_0,
        Keyboard.KEY_1,
        Keyboard.KEY_2,
        Keyboard.KEY_3,
        Keyboard.KEY_4,
        Keyboard.KEY_5,
        Keyboard.KEY_6,
        Keyboard.KEY_7,
        Keyboard.KEY_8,
        Keyboard.KEY_9,
    )
*/

    /* Key Code list?
    3 -- Cancel
8 -- Backspace
9 -- Tab
10 -- Enter
12 -- Clear
16 -- Shift
17 -- Ctrl
18 -- Alt
19 -- Pause
20 -- Caps Lock
21 -- Kana
24 -- Final
25 -- Kanji
27 -- Escape
28 -- Convert
29 -- No Convert
30 -- Accept
31 -- Mode Change
32 -- Space
33 -- Page Up
34 -- Page Down
35 -- End
36 -- Home
37 -- Left
38 -- Up
39 -- Right
40 -- Down
44 -- Comma
45 -- Minus
46 -- Period
47 -- Slash
48 -- 0
49 -- 1
50 -- 2
51 -- 3
52 -- 4
53 -- 5
54 -- 6
55 -- 7
56 -- 8
57 -- 9
59 -- Semicolon
61 -- Equals
65 -- A
66 -- B
67 -- C
68 -- D
69 -- E
70 -- F
71 -- G
72 -- H
73 -- I
74 -- J
75 -- K
76 -- L
77 -- M
78 -- N
79 -- O
80 -- P
81 -- Q
82 -- R
83 -- S
84 -- T
85 -- U
86 -- V
87 -- W
88 -- X
89 -- Y
90 -- Z
91 -- Open Bracket
92 -- Back Slash
93 -- Close Bracket
96 -- NumPad-0
97 -- NumPad-1
98 -- NumPad-2
99 -- NumPad-3
100 -- NumPad-4
101 -- NumPad-5
102 -- NumPad-6
103 -- NumPad-7
104 -- NumPad-8
105 -- NumPad-9
106 -- NumPad *
107 -- NumPad +
108 -- NumPad ,
109 -- NumPad -
110 -- NumPad .
111 -- NumPad /
112 -- F1
113 -- F2
114 -- F3
115 -- F4
116 -- F5
117 -- F6
118 -- F7
119 -- F8
120 -- F9
121 -- F10
122 -- F11
123 -- F12
127 -- Delete
128 -- Dead Grave
129 -- Dead Acute
130 -- Dead Circumflex
131 -- Dead Tilde
132 -- Dead Macron
133 -- Dead Breve
134 -- Dead Above Dot
135 -- Dead Diaeresis
136 -- Dead Above Ring
137 -- Dead Double Acute
138 -- Dead Caron
139 -- Dead Cedilla
140 -- Dead Ogonek
141 -- Dead Iota
142 -- Dead Voiced Sound
143 -- Dead Semivoiced Sound
144 -- Num Lock
145 -- Scroll Lock
150 -- Ampersand
151 -- Asterisk
152 -- Double Quote
153 -- Less
154 -- Print Screen
155 -- Insert
156 -- Help
157 -- Meta
160 -- Greater
161 -- Left Brace
162 -- Right Brace
192 -- Back Quote
222 -- Quote
224 -- Up
225 -- Down
226 -- Left
227 -- Right
240 -- Alphanumeric
241 -- Katakana
242 -- Hiragana
243 -- Full-Width
244 -- Half-Width
245 -- Roman Characters
256 -- All Candidates
257 -- Previous Candidate
258 -- Code Input
259 -- Japanese Katakana
260 -- Japanese Hiragana
261 -- Japanese Roman
262 -- Kana Lock
263 -- Input Method On/Off
512 -- At
513 -- Colon
514 -- Circumflex
515 -- Dollar
516 -- Euro
517 -- Exclamation Mark
518 -- Inverted Exclamation Mark
519 -- Left Parenthesis
520 -- Number Sign
521 -- Plus
522 -- Right Parenthesis
523 -- Underscore
524 -- Windows
525 -- Context Menu
61440 -- F13
61441 -- F14
61442 -- F15
61443 -- F16
61444 -- F17
61445 -- F18
61446 -- F19
61447 -- F20
61448 -- F21
61449 -- F22
61450 -- F23
61451 -- F24
65312 -- Compose
65368 -- Begin
65406 -- Alt Graph
65480 -- Stop
65481 -- Again
65482 -- Props
65483 -- Undo
65485 -- Copy
65487 -- Paste
65488 -- Find
65489 -- Cut
     */

    private val keyBlackList = intArrayOf(
        Keyboard.KEY_LSHIFT,
        Keyboard.KEY_RSHIFT,
        Keyboard.KEY_UP,
        Keyboard.KEY_RIGHT,
        Keyboard.KEY_LEFT,
        Keyboard.KEY_DOWN,
        Keyboard.KEY_END,
        Keyboard.KEY_NUMLOCK,
        Keyboard.KEY_DELETE,
        Keyboard.KEY_LCONTROL,
        Keyboard.KEY_RCONTROL,
        Keyboard.KEY_CAPITAL,
        Keyboard.KEY_LMENU,
        Keyboard.KEY_F1,
        Keyboard.KEY_F2,
        Keyboard.KEY_F3,
        Keyboard.KEY_F4,
        Keyboard.KEY_F5,
        Keyboard.KEY_F6,
        Keyboard.KEY_F7,
        Keyboard.KEY_F8,
        Keyboard.KEY_F9,
        Keyboard.KEY_F10,
        Keyboard.KEY_F11,
        Keyboard.KEY_F12,
        Keyboard.KEY_F13,
        Keyboard.KEY_F14,
        Keyboard.KEY_F15,
        Keyboard.KEY_F16,
        Keyboard.KEY_F17,
        Keyboard.KEY_F18,
        Keyboard.KEY_F19,
        Keyboard.KEY_SCROLL,
        Keyboard.KEY_RMENU,
        Keyboard.KEY_LMETA,
        Keyboard.KEY_RMETA,
        Keyboard.KEY_FUNCTION,
        Keyboard.KEY_PRIOR,
        Keyboard.KEY_NEXT,
        Keyboard.KEY_INSERT,
        Keyboard.KEY_HOME,
        Keyboard.KEY_PAUSE,
        Keyboard.KEY_APPS,
        Keyboard.KEY_POWER,
        Keyboard.KEY_SLEEP,
        Keyboard.KEY_SYSRQ,
        Keyboard.KEY_CLEAR,
        Keyboard.KEY_SECTION,
        Keyboard.KEY_UNLABELED,
        Keyboard.KEY_KANA,
        Keyboard.KEY_CONVERT,
        Keyboard.KEY_NOCONVERT,
        Keyboard.KEY_YEN,
        Keyboard.KEY_CIRCUMFLEX,
        Keyboard.KEY_AT,
        Keyboard.KEY_UNDERLINE,
        Keyboard.KEY_KANJI,
        Keyboard.KEY_STOP,
        Keyboard.KEY_AX,
        Keyboard.KEY_TAB,
    )

    /**
     * Rendering the element
     */
    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float) : Int{
        val displayValue = (setting as StringSetting).text
        val temp = ColorUtil.clickGUIColor
        if (listening) {
            val color = Color(temp.red, temp.green, temp.blue, 200).rgb
            Gui.drawRect(0, 0, settingWidth, settingHeight, color)
        }

        /** Rendering the text */
        if (FontUtil.getStringWidth(displayValue + "00" + setting.name) <= settingWidth) {
            FontUtil.drawString(setting.name, 1,  2, -0x1)
            FontUtil.drawString(displayValue, settingWidth - FontUtil.getStringWidth(displayValue), 2, -0x1)
        }else {
            if (isTextHovered(mouseX, mouseY) || listening) {
                FontUtil.drawCenteredStringWithShadow(displayValue, settingWidth / 2.0, 2.0, -0x1)
            } else {
                FontUtil.drawCenteredString(setting.name, settingWidth/ 2.0, 2.0, -0x1)
            }
        }

        return this.settingHeight

    }

    /**
     * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isTextHovered(mouseX, mouseY)) {
            listening = true
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Register key strokes.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                listening = false
            } else if (keyCode == Keyboard.KEY_BACK) {
                (setting as StringSetting).text = setting.text.dropLast(1)
            }else if (!keyBlackList.contains(keyCode)) {
                (setting as StringSetting).text = setting.text + typedChar.toString()
            }
            return true
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
     * Checks whether the mouse is hovering the text field
     */
    private fun isTextHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= parent.x + x && mouseX <= parent.x + x + settingWidth && mouseY >= parent.y + y  && mouseY <= parent.y + y + settingHeight
    }

}