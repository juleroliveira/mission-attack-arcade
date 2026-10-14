package com.arcade.missionattack

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object Sprites {

    val PALETTE: Map<Char, Int> = mapOf(
        '.' to Color.TRANSPARENT,
        'W' to 0xFFFFFFFF.toInt(),
        'C' to 0xFF38bdf8.toInt(),
        'B' to 0xFF0284c7.toInt(),
        'R' to 0xFFff2a55.toInt(),
        'D' to 0xFF881337.toInt(),
        'Y' to 0xFFfde047.toInt(),
        'O' to 0xFFf97316.toInt(),
        'P' to 0xFFc084fc.toInt(),
        'V' to 0xFF581c87.toInt(),
        'G' to 0xFF10b981.toInt(),
        'E' to 0xFF064e3b.toInt(),
        'S' to 0xFF64748b.toInt(),
        'K' to 0xFF334155.toInt(),
        'H' to 0xFF1e293b.toInt(),
        'M' to 0xFFf472b6.toInt(),
        'L' to 0xFFdb2777.toInt(),
        'm' to 0xFF131b2e.toInt(),
        'd' to 0xFF0c111d.toInt(),
        'n' to 0xFF19122b.toInt(),
        'p' to 0xFF24123b.toInt(),
        'v' to 0xFF422166.toInt(),
        'j' to 0xFF6b36a6.toInt(),
        'k' to 0xFF11061c.toInt(),
        'a' to 0xFFc084fc.toInt(),
        'c' to 0xFFe879f9.toInt(),
        'F' to 0xFFea580c.toInt(),
        'f' to 0xFF9a3412.toInt(),
        'y' to 0xFFfde047.toInt(),
        'o' to 0xFFf97316.toInt(),
        'x' to 0xFF170604.toInt(),
        'z' to 0xFF38140a.toInt()
    )

    fun createPixelSprite(
        matrix: Array<String>,
        pixelSize: Int = 3,
        overrideColor: Int? = null,
        hasOutline: Boolean = true
    ): Bitmap {
        val rows = matrix.size
        val cols = matrix[0].length
        val pad = 1
        val width = (cols + pad * 2) * pixelSize
        val height = (rows + pad * 2) * pixelSize

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        val dirs = arrayOf(
            intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1),
            intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(1, 1)
        )

        if (hasOutline && overrideColor == null) {
            val outlineColor = 0xFF060913.toInt()
            for (r in -1..rows) {
                for (c in -1..cols) {
                    val isFilled = (r in 0 until rows && c in 0 until cols && PALETTE[matrix[r][c]] != null && PALETTE[matrix[r][c]] != Color.TRANSPARENT)
                    if (isFilled) continue

                    var touchesSolid = false
                    for (d in dirs) {
                        val nr = r + d[0]
                        val nc = c + d[1]
                        if (nr in 0 until rows && nc in 0 until cols && PALETTE[matrix[nr][nc]] != null && PALETTE[matrix[nr][nc]] != Color.TRANSPARENT) {
                            touchesSolid = true
                            break
                        }
                    }

                    if (touchesSolid) {
                        fillPixelBlock(pixels, width, (c + pad) * pixelSize, (r + pad) * pixelSize, pixelSize, outlineColor)
                    }
                }
            }
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val ch = matrix[r][c]
                val color = overrideColor ?: (PALETTE[ch] ?: Color.TRANSPARENT)
                if (color != Color.TRANSPARENT) {
                    fillPixelBlock(pixels, width, (c + pad) * pixelSize, (r + pad) * pixelSize, pixelSize, color)
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun fillPixelBlock(pixels: IntArray, width: Int, startX: Int, startY: Int, size: Int, color: Int) {
        val height = pixels.size / width
        for (y in startY until startY + size) {
            if (y !in 0 until height) continue
            val rowOffset = y * width
            for (x in startX until startX + size) {
                if (x in 0 until width) {
                    pixels[rowOffset + x] = color
                }
            }
        }
    }

    // Matrizes fiéis
    val PLAYER_MATRIX = arrayOf(
        "....CCCCC....",
        "..CCCCCCCCC..",
        ".CCCCCCCCCCC.",
        "CC.CC.C.CC.CC",
        "CCCCCCCCCCCCC",
        ".CCCCCCCCCCC.",
        "..CC..C..CC..",
        "...C.....C..."
    )

    val INTERCEPTOR_MATRIX = arrayOf(
        "...RRRRR...",
        ".RRRRRRRRR.",
        "RR.RR.RR.RR",
        "RRRRRRRRRRR",
        ".RRR.R.RRR.",
        "..R.....R..",
        ".R.......R."
    )

    val GUNSHIP_MATRIX = arrayOf(
        "..P.....P..",
        "...PPPPP...",
        ".PPPPPPPPP.",
        "PP.PPPPP.PP",
        "PPPPPPPPPPP",
        ".PPPPPPPPP.",
        "..PP.P.PP..",
        ".P...P...P."
    )

    val CRUISER_MATRIX = arrayOf(
        "...GGGGGGG...",
        ".GGGGGGGGGGG.",
        "GG.GG.G.GG.GG",
        "GGGGGGGGGGGGG",
        "GGGGGGGGGGGGG",
        ".GGG.GGG.GGG.",
        "..G..G.G..G..",
        ".G.........G."
    )

    val SNIPER_MATRIX = arrayOf(
        "....YYY....",
        "..YYYYYYY..",
        ".YYY.Y.YYY.",
        "YYYYYYYYYYY",
        ".YYYYYYYYY.",
        "..Y..Y..Y..",
        ".Y...Y...Y."
    )

    val STRIKER_MATRIX = arrayOf(
        "......RR......",
        ".....RRRR.....",
        "....RRRRRR....",
        "...RROOYYORR..",
        "..RROOYYOORR..",
        ".RRRROOOORRRR.",
        "RRRR..YY..RRRR",
        "RR....WW....RR"
    )

    val VANGUARD_MATRIX = arrayOf(
        "..CCCCCC..",
        ".CCCCCCCCC.",
        "CC..BBB..CC",
        "C.BBBBBBB.C",
        "BBKKB.BKKBB",
        "BBKKBBBKKBB",
        ".BBBBBBBBB.",
        "..BB...BB.."
    )

    val MINELAYER_MATRIX = arrayOf(
        "...YYYYY...",
        ".YYHHYHHYY.",
        "YYHHYYYHHYY",
        "YYYYHYHYYYY",
        "YYHHYYYHHYY",
        ".YYHHYHHYY.",
        "...YYYYY..."
    )

    val MINE_MATRIX = arrayOf(
        "..RR..",
        ".RYYR.",
        "RYWYYR",
        "RYWYYR",
        ".RYYR.",
        "..RR.."
    )

    val SWARMER_MATRIX = arrayOf(
        "..VV..",
        ".VPPV.",
        "VPPPWV",
        ".VPPV.",
        "..VV.."
    )

    val LANCER_MATRIX = arrayOf(
        "....WWWW....",
        "...WCCCCW...",
        "..WCCYYCCW..",
        ".WKKYYYYKKW.",
        "WKKKSSSSKKKW",
        "WKKSSSSSSKKW",
        ".WKKK..KKKW.",
        "..W......W.."
    )

    val BARREL_MATRIX = arrayOf(
        ".RRRR.",
        "RDDRRD",
        "RRYYRR",
        "RRYYRR",
        "RDDRRD",
        ".RRRR."
    )

    val CRYSTAL_CLUSTER_MATRIX = arrayOf(
        "...CC...",
        "..CCCC..",
        ".CCWWCCC.",
        "CCCWWCCCC",
        "CCBCCBCCC",
        ".CBBCCBC.",
        "...CC..."
    )

    val TURRET_ALIVE = arrayOf(
        "...HHHHH...",
        ".HHDDDDDHH.",
        "HDRRRRRRRDH",
        "HDRRYYYRSSD",
        "HDRRYWYRWWW",
        "HDRRYWYRWWW",
        "HDRRYYYRSSD",
        "HDRRRRRRRDH",
        ".HHDDDDDHH.",
        "...HHHHH..."
    )

    val TURRET_DEAD = arrayOf(
        "...HHHHH...",
        ".HHdddddHH.",
        "Hddmm.mmddH",
        "Hdm..d..mdH",
        "Hd.dd.dd.dH",
        "Hd.dd.dd.dH",
        "Hdm..d..mdH",
        "Hddmm.mmddH",
        ".HHdddddHH.",
        "...HHHHH..."
    )

    val PLAT_TURRET_BASE = arrayOf(
        "...KKKKKKKK...",
        "..KSSSSSSSSK..",
        ".KSKKKKKKKKSK.",
        "KSKHHHHHHHHKSK",
        "KSKHBBBBBBHKSK",
        "KSKHBGGGGHBKSK",
        "KSKHBGGGGHBKSK",
        "KSKHBGGGGHBKSK",
        "KSKHBGGGGHBKSK",
        "KSKHBBBBBBHKSK",
        "KSKHHHHHHHHKSK",
        ".KSKKKKKKKKSK.",
        "..KSSSSSSSSK..",
        "...KKKKKKKK..."
    )

    val PLAT_TURRET_GUN = arrayOf(
        "..............",
        "....KKKKCCCCCC",
        "...KSSSSWWWWWW",
        "..KSGGGGSK....",
        "..KSGWWGSK....",
        "..KSGYYGSK....",
        "..KSGYYGSK....",
        "..KSGWWGSK....",
        "..KSGGGGSK....",
        "...KSSSSWWWWWW",
        "....KKKKCCCCCC",
        ".............."
    )

    val DRONE_MATRIX = arrayOf(
        ".CCC.",
        "CWWWC",
        "CWYWC",
        "CWWWC",
        ".CCC."
    )

    val PLAYER_MISSILE = arrayOf(
        "....RR....",
        "...RWWSR..",
        "..RWWSSR..",
        "..RWWSSR..",
        "..RWWSSR..",
        "..RRSSRR..",
        ".RROYYORR.",
        "R.RYWWYR.R",
        "...OYYO...",
        "....OO...."
    )

    val CLUSTER_BOMB = arrayOf(
        "..OOOO..",
        ".OFFFFO.",
        "OFFyyFFO",
        "OFyWWyFO",
        "OFyWWyFO",
        "OFFyyFFO",
        ".OFFFFO.",
        "..OOOO.."
    )

    fun createFortressBitmap(shapeIdx: Int, isShadow: Boolean = false): Bitmap {
        val grid = 60
        val pSize = 4
        val width = grid * pSize
        val height = grid * pSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val center = (grid - 1) / 2.0

        val shadowColor = 0x94000000.toInt()

        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val dx = abs(c - center)
                val dy = abs(r - center)
                val maxD = maxOf(dx, dy)
                val minD = minOf(dx, dy)
                var color: Int? = null

                when (shapeIdx % 5) {
                    0 -> { // Posto Sentinela Alfa (Cruz Compacta)
                        val inCenterBox = (dx <= 9.5 && dy <= 9.5)
                        val inCrossArm = (minD <= 5.5 && maxD <= 22.5)
                        if (inCenterBox || inCrossArm) {
                            color = when {
                                dx <= 4.0 && dy <= 4.0 -> null
                                minD < 1.2 && maxD <= 18.0 -> 0xFFFFFFFF.toInt()
                                minD < 2.0 && maxD <= 18.0 -> 0xFF38bdf8.toInt()
                                else -> 0xFF334155.toInt()
                            }
                        }
                    }
                    1 -> { // Couraçado Duplo H
                        val inSideWall = (dx in 15.5..26.5 && dy <= 27.5)
                        val inCenterBridge = (dx <= 16.5 && dy <= 8.5)
                        val inCoreBox = (dx <= 10.5 && dy <= 10.5)
                        if (inSideWall || inCenterBridge || inCoreBox) {
                            color = when {
                                dx <= 4.5 && dy <= 4.5 -> null
                                inSideWall && (dx < 16.8 || dx > 25.2 || dy > 26.2) -> 0xFF0f172a.toInt()
                                else -> 0xFF334155.toInt()
                            }
                        }
                    }
                    2 -> { // Estrela Imperial
                        val starArm = (dx * 0.42 + dy <= 12.2) || (dy * 0.42 + dx <= 12.2)
                        val innerDiamond = (dx + dy <= 23.5)
                        val coreVoid = (dx + dy <= 6.5)
                        if ((starArm || innerDiamond) && !coreVoid && maxD <= 28.5) {
                            color = when {
                                (dx < 1.2 && dy <= 25.0) || (dy < 1.2 && dx <= 25.0) -> 0xFFfde047.toInt()
                                abs(dx - dy) < 1.3 && (dx + dy) <= 21.0 -> 0xFF38bdf8.toInt()
                                else -> 0xFF475569.toInt()
                            }
                        }
                    }
                    3 -> { // Hexágono Bosconian
                        val hexMetric = maxOf(dy, dx * 0.85 + dy * 0.5, dx * 1.05)
                        val inOuterHex = (hexMetric in 17.5..26.5)
                        val inCross = ((dx <= 3.2 || dy <= 3.2) && hexMetric < 18.5)
                        val inCore = (dx <= 8.5 && dy <= 8.5)
                        if (inOuterHex || inCross || inCore) {
                            color = when {
                                dx <= 4.5 && dy <= 4.5 -> null
                                inOuterHex -> if (hexMetric > 23.6) 0xFF10b981.toInt() else 0xFF047857.toInt()
                                inCross && (dx < 1.2 || dy < 1.2) -> 0xFFfde047.toInt()
                                else -> 0xFF334155.toInt()
                            }
                        }
                    }
                    else -> { // Mega-Dreadnought X
                        val inCorner = (dx in 12.5..27.5 && dy in 12.5..27.5)
                        val inDiag = (abs(dx - dy) <= 5.2 && maxD <= 24.0)
                        val inCenter = (dx <= 11.5 && dy <= 11.5)
                        if (inCorner || inDiag || inCenter) {
                            color = when {
                                dx <= 4.5 && dy <= 4.5 -> null
                                abs(dx - dy) < 1.4 && maxD <= 22.0 -> 0xFFffffff.toInt()
                                abs(dx - dy) < 2.5 && maxD <= 22.0 -> 0xFFc084fc.toInt()
                                else -> 0xFF334155.toInt()
                            }
                        }
                    }
                }

                if (color != null) {
                    val finalColor = if (isShadow) shadowColor else color
                    fillPixelBlock(pixels, width, c * pSize, r * pSize, pSize, finalColor)
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
