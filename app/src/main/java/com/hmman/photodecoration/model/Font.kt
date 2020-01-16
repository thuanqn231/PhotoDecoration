package com.hmman.photodecoration.model

class Font {
    var color = 0
    var typeface: String? = null
    var size = 0f

    fun increaseSize(diff: Float) {
        size = size + diff
    }

    fun decreaseSize(diff: Float) {
        if (size - diff >= Limits.MIN_FONT_SIZE) {
            size = size - diff
        }
    }

    private interface Limits {
        companion object {
            const val MIN_FONT_SIZE = 0.01f
        }
    }
}