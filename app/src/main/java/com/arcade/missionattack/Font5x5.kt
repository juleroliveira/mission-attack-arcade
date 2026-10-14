package com.arcade.missionattack

import android.graphics.Canvas
import android.graphics.Paint
import java.text.Normalizer

object Font5x5 {

    private val GLYPHS = mapOf(
        'A' to arrayOf(".###.", "#####", "#...#", "#####", "#...#"),
        'B' to arrayOf("####.", "#...#", "####.", "#...#", "####."),
        'C' to arrayOf(".####", "#....", "#....", "#....", ".####"),
        'D' to arrayOf("####.", "#...#", "#...#", "#...#", "####."),
        'E' to arrayOf("#####", "#....", "####.", "#....", "#####"),
        'F' to arrayOf("#####", "#....", "####.", "#....", "#...."),
        'G' to arrayOf(".####", "#....", "#..##", "#...#", ".###."),
        'H' to arrayOf("#...#", "#...#", "#####", "#...#", "#...#"),
        'I' to arrayOf("#####", "..#..", "..#..", "..#..", "#####"),
        'J' to arrayOf("..###", "...#.", "...#.", "#..#.", ".###."),
        'K' to arrayOf("#..#.", "#.#..", "##...", "#.#..", "#..#."),
        'L' to arrayOf("#....", "#....", "#....", "#....", "#####"),
        'M' to arrayOf("#...#", "##.##", "#.#.#", "#...#", "#...#"),
        'N' to arrayOf("#...#", "##..#", "#.#.#", "#..##", "#...#"),
        'O' to arrayOf(".###.", "#...#", "#...#", "#...#", ".###."),
        'P' to arrayOf("####.", "#...#", "####.", "#....", "#...."),
        'Q' to arrayOf(".###.", "#...#", "#.#.#", "#..#.", ".##.#"),
        'R' to arrayOf("####.", "#...#", "####.", "#..#.", "#...#"),
        'S' to arrayOf(".####", "#....", ".###.", "....#", "####."),
        'T' to arrayOf("#####", "..#..", "..#..", "..#..", "..#.."),
        'U' to arrayOf("#...#", "#...#", "#...#", "#...#", ".###."),
        'V' to arrayOf("#...#", "#...#", "#...#", ".#.#.", "..#.."),
        'W' to arrayOf("#...#", "#...#", "#.#.#", "##.##", "#...#"),
        'X' to arrayOf("#...#", ".#.#.", "..#..", ".#.#.", "#...#"),
        'Y' to arrayOf("#...#", ".#.#.", "..#..", "..#..", "..#.."),
        'Z' to arrayOf("#####", "...#.", "..#..", ".#...", "#####"),
        '0' to arrayOf(".###.", "#..##", "#.#.#", "##..#", ".###."),
        '1' to arrayOf("..#..", ".##..", "..#..", "..#..", "#####"),
        '2' to arrayOf(".###.", "#...#", "..##.", ".#...", "#####"),
        '3' to arrayOf("####.", "....#", ".###.", "....#", "####."),
        '4' to arrayOf("#..#.", "#..#.", "#####", "...#.", "...#."),
        '5' to arrayOf("#####", "#....", "####.", "....#", "####."),
        '6' to arrayOf(".###.", "#....", "####.", "#...#", ".###."),
        '7' to arrayOf("#####", "...#.", "..#..", ".#...", ".#..."),
        '8' to arrayOf(".###.", "#...#", ".###.", "#...#", ".###."),
        '9' to arrayOf(".###.", "#...#", ".####", "....#", ".###."),
        ':' to arrayOf(".....", "..#..", ".....", "..#..", "....."),
        '-' to arrayOf(".....", ".....", "#####", ".....", "....."),
        '+' to arrayOf(".....", "..#..", ".###.", "..#..", "....."),
        '!' to arrayOf("..#..", "..#..", "..#..", ".....", "..#.."),
        '?' to arrayOf(".###.", "#...#", "..##.", ".....", "..#.."),
        '/' to arrayOf("....#", "...#.", "..#..", ".#...", "#...."),
        '%' to arrayOf("##..#", "##.#.", "..#..", ".#.##", "#..##"),
        '.' to arrayOf(".....", ".....", ".....", ".....", "..#.."),
        ',' to arrayOf(".....", ".....", ".....", "..#..", ".#..."),
        '>' to arrayOf(".#...", "..#..", "...#.", "..#..", ".#..."),
        '<' to arrayOf("...#.", "..#..", ".#...", "..#..", "...#."),
        '[' to arrayOf(".###.", ".#...", ".#...", ".#...", ".###."),
        ']' to arrayOf(".###.", "...#.", "...#.", "...#.", ".###."),
        ' ' to arrayOf(".....", ".....", ".....", ".....", ".....")
    )

    private val SPACE_GLYPH = arrayOf(".....", ".....", ".....", ".....", ".....")

    fun clean(text: String): String {
        return Normalizer.normalize(text.uppercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun measure(text: String, scale: Int = 2): Float {
        val clean = clean(text)
        if (clean.isEmpty()) return 0f
        return (clean.length * (6 * scale) - scale).toFloat()
    }

    fun draw(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        scale: Int = 2,
        color: Int = 0xFFFFFFFF.toInt(),
        align: String = "left",
        shadowColor: Int? = 0xFF000000.toInt(),
        maxWidth: Float? = null
    ) {
        val clean = clean(text)
        if (clean.isEmpty()) return

        var finalScale = scale
        if (maxWidth != null) {
            while (finalScale > 1 && measure(clean, finalScale) > maxWidth) {
                finalScale--
            }
        }

        val charW = 5 * finalScale
        val spacing = 1 * finalScale
        val totalW = clean.length * (charW + spacing) - spacing

        var startX = when (align) {
            "center" -> x - totalW / 2f
            "right" -> x - totalW.toFloat()
            else -> x
        }
        val startY = y

        for (i in clean.indices) {
            val ch = clean[i]
            val glyph = GLYPHS[ch] ?: SPACE_GLYPH
            val cx = startX + i * (charW + spacing)

            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    if (glyph[r][c] == '#') {
                        val px = cx + c * finalScale
                        val py = startY + r * finalScale

                        if (shadowColor != null) {
                            paint.color = shadowColor
                            canvas.drawRect(
                                px + finalScale,
                                py + finalScale,
                                px + finalScale * 2f,
                                py + finalScale * 2f,
                                paint
                            )
                        }
                        paint.color = color
                        canvas.drawRect(
                            px,
                            py,
                            px + finalScale.toFloat(),
                            py + finalScale.toFloat(),
                            paint
                        )
                    }
                }
            }
        }
    }
}
