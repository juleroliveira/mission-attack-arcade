package com.arcade.missionattack

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*

@SuppressLint("ViewConstructor")
class GameView(context: Context, val soundEngine: SoundEngine) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    var screenState = ScreenState.MENU
    private var previousState = ScreenState.MENU

    private var screenW = 720f
    private var screenH = 1280f
    private var virtualW = 480f
    private var virtualH = 880f
    private var scale = 1.5f

    private val worldSize = 9600f

    // Nave do Jogador
    var playerX = worldSize / 2f
    var playerY = worldSize / 2f
    var playerAngle = -PI.toFloat() / 2f
    var playerSpeed = 5.4f
    var playerVx = 0f
    var playerVy = -playerSpeed
    val playerRadius = 18f
    var playerHp = 100f
    var playerMaxHp = 100f
    var playerLevel = 1
    var playerXp = 0
    var playerNextXp = 20
    var bulletDamage = 30f
    var bulletCount = 1
    var fireRateMs = 215L
    var lastFireTime = 0L
    var kills = 0
    var sectorLevel = 1
    var selectedSector = 1

    // Hiper-Velocidade
    var isDashing = false
    var dashTimer = 0
    var dashCooldown = 0
    private var dashVx = 0f
    private var dashVy = 0f

    // Controles Touch
    private var touchStart: PointF? = null
    private var touchCurrent: PointF? = null
    private var moveDir = PointF(0f, -1f)

    // Entidades
    private val uiButtons = mutableListOf<UiButton>()
    private val bullets = mutableListOf<Bullet>()
    private val enemyBullets = mutableListOf<EnemyBullet>()
    private val enemies = mutableListOf<Enemy>()
    private val gems = mutableListOf<Gem>()
    private val particles = mutableListOf<Particle>()
    private val bases = mutableListOf<BaseStation>()
    private val platformTurrets = mutableListOf<PlatformTurret>()
    private val platforms = mutableListOf<StingerPlatform>()
    private val stars = mutableListOf<Star>()
    private val warpStars = mutableListOf<WarpStar>()

    // Sprites
    private val sprPlayer = Sprites.createPixelSprite(Sprites.PLAYER_MATRIX, 3)
    private val sprPlayerHitRed = Sprites.createPixelSprite(Sprites.PLAYER_MATRIX, 3, 0xFFff2a55.toInt())
    private val sprInterceptor = Sprites.createPixelSprite(Sprites.INTERCEPTOR_MATRIX, 3)
    private val sprGunship = Sprites.createPixelSprite(Sprites.GUNSHIP_MATRIX, 3)
    private val sprCruiser = Sprites.createPixelSprite(Sprites.CRUISER_MATRIX, 3)
    private val sprSniper = Sprites.createPixelSprite(Sprites.SNIPER_MATRIX, 3)
    private val sprStriker = Sprites.createPixelSprite(Sprites.STRIKER_MATRIX, 3)
    private val sprTurretAlive = Sprites.createPixelSprite(Sprites.TURRET_ALIVE, 3)
    private val sprTurretDead = Sprites.createPixelSprite(Sprites.TURRET_DEAD, 3)
    private val sprPlatBase = Sprites.createPixelSprite(Sprites.PLAT_TURRET_BASE, 3)
    private val sprPlatGun = Sprites.createPixelSprite(Sprites.PLAT_TURRET_GUN, 3)
    private val stationSprites = Array(5) { idx -> Sprites.createFortressBitmap(idx, false) }

    var enableCrt = true
    var enableShake = true
    private var shakePower = 0f
    private var shakeX = 0f
    private var shakeY = 0f
    private var hurtFlash = 0

    private var warpTimer = 110
    private val warpMaxTime = 110

    data class SectorInfo(val id: Int, val tag: String, val full: String, val desc: String, val col: Int)
    private val sectorInfos = arrayOf(
        SectorInfo(1, "FASE 1: ORBITA", "ORBITA SIDERAL", "ESPACO PROFUNDO - KAMIKAZE STRIKER", 0xFF38bdf8.toInt()),
        SectorInfo(2, "FASE 2: PLANETA", "SUPERFICIE VULCANICA", "PLANETA DE MAGMA - BARRIS & MINAS", 0xFFf97316.toInt()),
        SectorInfo(3, "FASE 3: TRINCHEIRA", "CORREDOR DE TURRETS", "CORREDOR MILITAR - PORTAO LASER", 0xFFfde047.toInt()),
        SectorInfo(4, "FASE 4: CAVERNA", "CAVERNA SUBTERRANEA", "CAMARAS DE ROCHA - CRISTAIS", 0xFFc084fc.toInt()),
        SectorInfo(5, "FASE 5: CIDADELA", "CIDADELA SUPREMA", "MEGA-DREADNOUGHT - BASE IMPERIAL", 0xFF10b981.toInt())
    )

    init {
        holder.addCallback(this)
        isFocusable = true
        soundEngine.startMusic()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        updateDimensions(width, height)
        initStars()
        initWarpStars()
        initBases()
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateDimensions(width, height)
    }

    private fun updateDimensions(w: Int, h: Int) {
        screenW = w.toFloat()
        screenH = h.toFloat()
        virtualW = 480f
        scale = screenW / virtualW
        virtualH = screenH / scale
        initWarpStars()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        soundEngine.stopMusic()
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
        }
    }

    private fun initStars() {
        stars.clear()
        val random = java.util.Random()
        for (i in 0 until 80) {
            stars.add(
                Star(
                    random.nextFloat() * virtualW,
                    random.nextFloat() * virtualH,
                    0.15f + random.nextFloat() * 0.5f,
                    if (random.nextBoolean()) 2f else 3f,
                    if (random.nextBoolean()) 0xFF334155.toInt() else 0xFF1e293b.toInt()
                )
            )
        }
    }

    private fun initWarpStars() {
        warpStars.clear()
        val random = java.util.Random()
        val colors = intArrayOf(
            0xFF1e3a8a.toInt(), 0xFF2563eb.toInt(), 0xFF38bdf8.toInt(),
            0xFF67e8f9.toInt(), 0xFFffffff.toInt(), 0xFFfde047.toInt()
        )
        for (i in 0 until 400) {
            warpStars.add(
                WarpStar(
                    random.nextFloat() * virtualW,
                    random.nextFloat() * virtualH,
                    16f + random.nextFloat() * 32f,
                    if (random.nextBoolean()) 2f else 3f,
                    40f + random.nextFloat() * 100f,
                    colors[random.nextInt(colors.size)]
                )
            )
        }
    }

    private fun initBases() {
        bases.clear()
        platformTurrets.clear()
        platforms.clear()

        val stationCount = min(5, 1 + sectorLevel)
        val shapeIdx = (sectorLevel - 1) % 5

        for (i in 0 until stationCount) {
            val ang = (i.toFloat() / stationCount) * PI.toFloat() * 2f + 0.4f
            val dist = 1600f + i * 450f
            val bx = wrapCoord(playerX + cos(ang) * dist)
            val by = wrapCoord(playerY + sin(ang) * dist)

            val pods = mutableListOf(
                Pod(0, 72f, -26f, 0f, 600f, 600f),
                Pod(1, 72f, 26f, 0f, 600f, 600f),
                Pod(2, -72f, 26f, PI.toFloat(), 600f, 600f),
                Pod(3, -72f, -26f, PI.toFloat(), 600f, 600f)
            )

            bases.add(
                BaseStation(
                    bx, by, shapeIdx, "FORTALEZA SETOR $sectorLevel", 92f, 2400f, 2400f, 24f, 0, 0f, pods
                )
            )

            val pObj = StingerPlatform(
                bx, by, 1200f, 1200f, "cross",
                listOf(
                    PlatformRect(0f, 0f, 1200f, 450f),
                    PlatformRect(0f, 0f, 450f, 1200f)
                )
            )
            platforms.add(pObj)

            platformTurrets.add(PlatformTurret(wrapCoord(bx + 400f), by, 380f, 380f))
            platformTurrets.add(PlatformTurret(wrapCoord(bx - 400f), by, 380f, 380f))
            platformTurrets.add(PlatformTurret(bx, wrapCoord(by + 400f), 380f, 380f))
            platformTurrets.add(PlatformTurret(bx, wrapCoord(by - 400f), 380f, 380f))
        }
    }

    private fun wrapCoord(v: Float): Float {
        return ((v % worldSize) + worldSize) % worldSize
    }

    private fun getDelta(from: Float, to: Float): Float {
        var d = to - from
        if (d > worldSize / 2f) d -= worldSize
        if (d < -worldSize / 2f) d += worldSize
        return d
    }

    private fun getDist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return hypot(getDelta(x1, x2), getDelta(y1, y2))
    }

    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return atan2(getDelta(y1, y2), getDelta(x1, x2))
    }

    private fun toScreen(wx: Float, wy: Float): PointF {
        return PointF(
            virtualW / 2f + getDelta(playerX, wx) + shakeX,
            virtualH / 2f + getDelta(playerY, wy) + shakeY
        )
    }

    fun startNewGame(keepProgress: Boolean = false, sector: Int? = null) {
        sectorLevel = sector ?: if (!keepProgress) selectedSector else (sectorLevel + 1)
        if (!keepProgress) {
            playerLevel = 1
            playerXp = 0
            playerNextXp = 20
            playerHp = 100f
            kills = 0
            bulletCount = if (sectorLevel >= 3) 2 else 1
        }
        playerX = worldSize / 2f
        playerY = worldSize / 2f
        playerAngle = -PI.toFloat() / 2f
        playerVx = 0f
        playerVy = -playerSpeed
        moveDir = PointF(0f, -1f)
        bullets.clear()
        enemyBullets.clear()
        enemies.clear()
        gems.clear()
        particles.clear()
        initBases()

        warpTimer = warpMaxTime
        screenState = ScreenState.WARP_IN
        soundEngine.playSfx("warpIn")
    }

    fun triggerHyperVelocity(): Boolean {
        if (screenState != ScreenState.PLAYING || dashCooldown > 0 || isDashing) return false
        val dashAng = atan2(moveDir.y, moveDir.x)
        val dashSpeed = playerSpeed * 2.85f
        isDashing = true
        dashTimer = 14
        dashCooldown = 70
        dashVx = cos(dashAng) * dashSpeed
        dashVy = sin(dashAng) * dashSpeed
        soundEngine.playSfx("hyperDash")
        addShake(7f)
        return true
    }

    fun addShake(pow: Float) {
        if (!enableShake) return
        shakePower = max(shakePower, pow).coerceAtMost(24f)
    }

    override fun run() {
        var lastTime = System.currentTimeMillis()
        while (isRunning) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTime
            if (elapsed < 16) {
                try {
                    Thread.sleep(16 - elapsed)
                } catch (e: InterruptedException) {
                }
            }
            lastTime = System.currentTimeMillis()

            update(now)
            render(now)
        }
    }

    private fun autoShoot(now: Long) {
        var closestTarget: PointF? = null
        var minD = 420f

        // 1. Inimigos Visíveis na Tela
        for (e in enemies) {
            val pos = toScreen(e.x, e.y)
            if (pos.x < 10f || pos.x > virtualW - 10f || pos.y < 10f || pos.y > virtualH - 10f) continue
            val d = getDist(playerX, playerY, e.x, e.y)
            val weight = if (e.isCharging) 0.6f else 1.0f
            if (d * weight < minD) {
                minD = d * weight
                closestTarget = PointF(e.x, e.y)
            }
        }

        // 2. Torretas de Plataforma
        if (closestTarget == null || minD > 280f) {
            for (pt in platformTurrets) {
                if (pt.alive) {
                    val pos = toScreen(pt.x, pt.y)
                    if (pos.x >= 15f && pos.x <= virtualW - 15f && pos.y >= 15f && pos.y <= virtualH - 15f) {
                        val d = getDist(playerX, playerY, pt.x, pt.y)
                        if (d < minD && d < 300f) {
                            minD = d
                            closestTarget = PointF(pt.x, pt.y)
                        }
                    }
                }
            }
        }

        // 3. Pods de Fortalezas Espaciais
        if (closestTarget == null || minD > 260f) {
            for (b in bases) {
                if (b.dying) continue
                val alivePods = b.pods.filter { it.alive }
                if (alivePods.isNotEmpty()) {
                    for (pod in alivePods) {
                        val px = wrapCoord(b.x + pod.lx)
                        val py = wrapCoord(b.y + pod.ly)
                        val pos = toScreen(px, py)
                        if (pos.x >= 20f && pos.x <= virtualW - 20f && pos.y >= 20f && pos.y <= virtualH - 20f) {
                            val d = getDist(playerX, playerY, px, py)
                            if (d < minD && d < 280f) {
                                minD = d
                                closestTarget = PointF(px, py)
                            }
                        }
                    }
                } else {
                    val pos = toScreen(b.x, b.y)
                    if (pos.x >= 25f && pos.x <= virtualW - 25f && pos.y >= 25f && pos.y <= virtualH - 25f) {
                        val dc = getDist(playerX, playerY, b.x, b.y)
                        if (dc < minD && dc < 260f) {
                            minD = dc
                            closestTarget = PointF(b.x, b.y)
                        }
                    }
                }
            }
        }

        // DISPARO AUTOMÁTICO DIRECIONADO AO ALVO + TIRO TRASEIRO DEFENSIVO
        if (closestTarget != null && now - lastFireTime >= fireRateMs) {
            lastFireTime = now
            val fireAng = getAngle(playerX, playerY, closestTarget.x, closestTarget.y)
            soundEngine.playSfx("laser")

            val count = min(3, bulletCount)
            val spread = 0.15f
            for (i in 0 until count) {
                val ang = fireAng + (i - (count - 1) / 2f) * spread
                bullets.add(
                    Bullet(
                        playerX, playerY,
                        cos(ang) * 11.5f, sin(ang) * 11.5f,
                        ang, 450f, bulletDamage, 6f
                    )
                )
            }

            // Tiro traseiro defensivo
            val backAng = fireAng + PI.toFloat()
            bullets.add(
                Bullet(
                    playerX, playerY,
                    cos(backAng) * 10f, sin(backAng) * 10f,
                    backAng, 320f, bulletDamage * 0.75f, 5f
                )
            )
        }
    }

    private fun update(now: Long) {
        if (shakePower > 0.2f) {
            shakeX = (Math.random().toFloat() - 0.5f) * shakePower
            shakeY = (Math.random().toFloat() - 0.5f) * shakePower
            shakePower *= 0.85f
        } else {
            shakeX = 0f
            shakeY = 0f
            shakePower = 0f
        }

        if (hurtFlash > 0) hurtFlash--

        if (screenState == ScreenState.WARP_IN) {
            warpTimer--
            if (warpTimer <= 0) {
                screenState = ScreenState.PLAYING
                playerAngle = -PI.toFloat() / 2f
                moveDir = PointF(0f, -1f)
                playerVx = 0f
                playerVy = -playerSpeed
            }
            return
        }

        if (screenState != ScreenState.PLAYING) return

        if (isDashing) {
            dashTimer--
            playerVx = dashVx
            playerVy = dashVy
            playerX = wrapCoord(playerX + playerVx)
            playerY = wrapCoord(playerY + playerVy)
            if (dashTimer <= 0) isDashing = false
        } else {
            playerVx = playerVx * 0.88f + (moveDir.x * playerSpeed) * 0.12f
            playerVy = playerVy * 0.88f + (moveDir.y * playerSpeed) * 0.12f
            playerX = wrapCoord(playerX + playerVx)
            playerY = wrapCoord(playerY + playerVy)
        }

        if (dashCooldown > 0) dashCooldown--

        autoShoot(now)

        val bIt = bullets.iterator()
        while (bIt.hasNext()) {
            val b = bIt.next()
            b.x = wrapCoord(b.x + b.vx)
            b.y = wrapCoord(b.y + b.vy)
            b.range -= hypot(b.vx, b.vy)

            var hit = false
            for (e in enemies) {
                if (getDist(b.x, b.y, e.x, e.y) < b.size + e.r) {
                    e.hp -= b.damage
                    e.hitFlash = 3
                    soundEngine.playSfx("hit")
                    hit = true
                    break
                }
            }

            if (!hit) {
                for (pt in platformTurrets) {
                    if (pt.alive && getDist(b.x, b.y, pt.x, pt.y) < b.size + 22f) {
                        pt.hp -= b.damage
                        pt.hitFlash = 3
                        soundEngine.playSfx("hit")
                        if (pt.hp <= 0) {
                            pt.alive = false
                            soundEngine.playSfx("turretBoom")
                            addShake(11f)
                            kills += 2
                            gems.add(Gem(pt.x, pt.y, 5, 0))
                        }
                        hit = true
                        break
                    }
                }
            }

            if (!hit) {
                for (base in bases) {
                    if (base.dying) continue
                    for (pod in base.pods) {
                        if (pod.alive) {
                            val px = wrapCoord(base.x + pod.lx)
                            val py = wrapCoord(base.y + pod.ly)
                            if (getDist(b.x, b.y, px, py) < b.size + 18f) {
                                pod.hp -= b.damage
                                pod.hitFlash = 3
                                soundEngine.playSfx("hit")
                                if (pod.hp <= 0) {
                                    pod.alive = false
                                    soundEngine.playSfx("turretBoom")
                                    addShake(11f)
                                    gems.add(Gem(px, py, 4, 0))
                                }
                                hit = true
                                break
                            }
                        }
                    }
                    if (!hit && base.pods.none { it.alive } && getDist(b.x, b.y, base.x, base.y) < b.size + base.coreR) {
                        base.coreHp -= b.damage
                        soundEngine.playSfx("hit")
                        if (base.coreHp <= 0) {
                            base.dying = true
                            soundEngine.playSfx("cataclysm")
                            addShake(20f)
                            kills += 10
                        }
                        hit = true
                    }
                    if (hit) break
                }
            }

            if (hit || b.range <= 0) bIt.remove()
        }

        for (pt in platformTurrets) {
            if (pt.alive && getDist(playerX, playerY, pt.x, pt.y) < 490f && now - pt.lastShot > 2300L) {
                pt.lastShot = now
                val aim = getAngle(pt.x, pt.y, playerX, playerY)
                enemyBullets.add(
                    EnemyBullet(
                        pt.x, pt.y,
                        cos(aim) * 4.8f, sin(aim) * 4.8f,
                        aim, 490f, 11f, 0xFF10b981.toInt()
                    )
                )
            }
        }

        if (enemies.size < 6 && Math.random() < 0.04) {
            val sAng = (Math.random() * PI * 2).toFloat()
            val types = arrayOf("interceptor", "gunship", "striker")
            val type = types[(Math.random() * types.size).toInt()]
            enemies.add(
                Enemy(
                    type,
                    wrapCoord(playerX + cos(sAng) * 600f),
                    wrapCoord(playerY + sin(sAng) * 600f),
                    sAng + PI.toFloat(),
                    if (type == "striker") 2.8f else 2.2f,
                    if (type == "gunship") 180f else 75f,
                    if (type == "gunship") 180f else 75f,
                    18f,
                    if (type == "striker") 0xFFf97316.toInt() else (if (type == "gunship") 0xFFc084fc.toInt() else 0xFFff2a55.toInt()),
                    if (type == "gunship") 2 else 1,
                    if (type == "gunship") 2600L else 3200L
                )
            )
        }

        val eIt = enemies.iterator()
        while (eIt.hasNext()) {
            val e = eIt.next()
            val angToP = getAngle(e.x, e.y, playerX, playerY)
            val distToP = getDist(e.x, e.y, playerX, playerY)

            if (e.type == "striker" && !e.isCharging && distToP < 380f) {
                e.isCharging = true
                e.chargeAngle = angToP
                soundEngine.playSfx("countdownBeep")
            }

            if (e.isCharging) {
                e.x = wrapCoord(e.x + cos(e.chargeAngle) * 4.8f)
                e.y = wrapCoord(e.y + sin(e.chargeAngle) * 4.8f)
                e.angle = e.chargeAngle
            } else {
                e.x = wrapCoord(e.x + cos(angToP) * e.speed)
                e.y = wrapCoord(e.y + sin(angToP) * e.speed)
                e.angle = angToP
            }

            if (distToP < 440f && now - e.lastShot > e.shotDelay) {
                e.lastShot = now
                soundEngine.playSfx("hit")
                enemyBullets.add(
                    EnemyBullet(
                        e.x, e.y,
                        cos(angToP) * 4.4f, sin(angToP) * 4.4f,
                        angToP, 410f, 8f, e.color
                    )
                )
            }

            if (distToP < playerRadius + e.r) {
                playerHp -= if (e.isCharging) 24f else 12f
                hurtFlash = 12
                soundEngine.playSfx("hit")
                addShake(8f)
                e.hp -= 40f
            }

            if (e.hp <= 0) {
                soundEngine.playSfx("explosion")
                addShake(6f)
                kills++
                gems.add(Gem(e.x, e.y, e.xp, (Math.random() * 10).toInt()))
                eIt.remove()
            }
        }

        val ebIt = enemyBullets.iterator()
        while (ebIt.hasNext()) {
            val eb = ebIt.next()
            eb.x = wrapCoord(eb.x + eb.vx)
            eb.y = wrapCoord(eb.y + eb.vy)
            eb.range -= hypot(eb.vx, eb.vy)

            if (getDist(playerX, playerY, eb.x, eb.y) < playerRadius + 4f) {
                playerHp -= eb.damage
                hurtFlash = 12
                soundEngine.playSfx("hit")
                addShake(8f)
                ebIt.remove()
                continue
            }
            if (eb.range <= 0) ebIt.remove()
        }

        val gIt = gems.iterator()
        while (gIt.hasNext()) {
            val g = gIt.next()
            val d = getDist(playerX, playerY, g.x, g.y)
            if (d < 135f) {
                val ang = getAngle(g.x, g.y, playerX, playerY)
                g.x = wrapCoord(g.x + cos(ang) * 5.8f)
                g.y = wrapCoord(g.y + sin(ang) * 5.8f)
            }
            if (d < playerRadius + 12f) {
                soundEngine.playSfx("xp")
                playerXp += g.xp
                if (playerXp >= playerNextXp) {
                    playerXp -= playerNextXp
                    playerLevel++
                    playerNextXp += 15
                    bulletCount = min(3, bulletCount + 1)
                    soundEngine.playSfx("lvlup")
                }
                gIt.remove()
            }
        }

        if (playerHp <= 0) {
            playerHp = 0f
            soundEngine.playSfx("explosion")
            screenState = ScreenState.GAMEOVER
        }
    }

    private fun render(now: Long) {
        val canvas = holder.lockHardwareCanvas() ?: return
        try {
            uiButtons.clear()

            // Escala Virtual Arcade Proporcional
            canvas.save()
            canvas.scale(scale, scale)

            canvas.drawColor(0xFF030614.toInt())

            when (screenState) {
                ScreenState.MENU -> renderMenu(canvas, now)
                ScreenState.WARP_IN -> renderWarpIn(canvas, now)
                ScreenState.PLAYING -> renderPlaying(canvas, now)
                ScreenState.PAUSED -> renderPaused(canvas)
                ScreenState.GAMEOVER -> renderGameOver(canvas)
                ScreenState.SETTINGS -> renderSettings(canvas)
                ScreenState.HOWTO -> renderHowTo(canvas)
            }

            if (enableCrt) {
                paint.color = 0x24000000.toInt()
                paint.strokeWidth = 2f
                var y = 0f
                while (y < virtualH) {
                    canvas.drawLine(0f, y, virtualW, y, paint)
                    y += 4f
                }
            }

            canvas.restore()

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun renderMenu(canvas: Canvas, now: Long) {
        paint.color = 0xFF38bdf8.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(18f, 20f, virtualW - 18f, virtualH - 20f, paint)
        paint.style = Paint.Style.FILL

        Font5x5.draw(canvas, paint, "MISSION:", virtualW / 2f, 46f, 4, 0xFFfde047.toInt(), "center")
        Font5x5.draw(canvas, paint, "ATTACK !", virtualW / 2f, 74f, 5, 0xFFff2a55.toInt(), "center")
        Font5x5.draw(canvas, paint, "1984 ARCADE EDITION", virtualW / 2f, 104f, 2, 0xFF38bdf8.toInt(), "center")

        val currentSecIdx = (selectedSector - 1).coerceIn(0, 4)
        val secInfo = sectorInfos[currentSecIdx]

        val currentStationSpr = stationSprites[currentSecIdx]
        canvas.save()
        canvas.translate(virtualW / 2f, 200f)
        canvas.rotate((now * 0.05f) % 360f)
        canvas.drawBitmap(currentStationSpr, -currentStationSpr.width / 2f, -currentStationSpr.height / 2f, paint)

        for (i in 0 until 4) {
            val pAng = i * (PI.toFloat() / 2f)
            val px = cos(pAng) * 65f
            val py = sin(pAng) * 65f
            canvas.drawBitmap(sprTurretAlive, px - sprTurretAlive.width / 2f, py - sprTurretAlive.height / 2f, paint)
        }
        canvas.restore()

        Font5x5.draw(canvas, paint, "ALVO: ${secInfo.full}", virtualW / 2f, 268f, 2, secInfo.col, "center")
        Font5x5.draw(canvas, paint, "[ 4 CANHOES DEFENSIVOS ]", virtualW / 2f, 288f, 2, 0xFFfde047.toInt(), "center")

        val btnW = min(virtualW - 56f, 310f)
        val bx = virtualW / 2f - btnW / 2f

        drawPixelButton(canvas, bx, 316f, btnW, 44f, "<  ${secInfo.tag}  >", 0xFF060b17.toInt(), secInfo.col, secInfo.col) {
            selectedSector = if (selectedSector < 5) selectedSector + 1 else 1
            soundEngine.playSfx("click")
        }

        Font5x5.draw(canvas, paint, secInfo.desc, virtualW / 2f, 372f, 2, 0xFF94a3b8.toInt(), "center")

        drawPixelButton(canvas, bx, 410f, btnW, 46f, "INICIAR MISSAO", 0xFF10b981.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            startNewGame(false, selectedSector)
        }

        drawPixelButton(canvas, bx, 464f, btnW, 42f, "CONFIGURACOES", 0xFF1e293b.toInt(), 0xFF38bdf8.toInt(), 0xFF38bdf8.toInt()) {
            previousState = ScreenState.MENU
            screenState = ScreenState.SETTINGS
            soundEngine.playSfx("click")
        }

        drawPixelButton(canvas, bx, 514f, btnW, 42f, "COMO JOGAR", 0xFF1e293b.toInt(), 0xFFfde047.toInt(), 0xFFfde047.toInt()) {
            screenState = ScreenState.HOWTO
            soundEngine.playSfx("click")
        }

        Font5x5.draw(canvas, paint, "USE <- / -> OU [A / D] PARA ESCOLHER FASE", virtualW / 2f, 574f, 2, 0xFF64748b.toInt(), "center")
    }

    private fun renderWarpIn(canvas: Canvas, now: Long) {
        paint.color = 0xFF02040b.toInt()
        canvas.drawRect(0f, 0f, virtualW, virtualH, paint)

        for (ws in warpStars) {
            ws.y += ws.speed
            if (ws.y > virtualH) ws.y = 0f
            paint.color = ws.color
            canvas.drawRect(ws.x, ws.y, ws.x + ws.width, ws.y + ws.baseLen, paint)
        }

        val t = 1f - warpTimer.toFloat() / warpMaxTime
        val shipY = virtualH * 0.88f - t * (virtualH * 0.38f)
        canvas.drawBitmap(sprPlayer, virtualW / 2f - sprPlayer.width / 2f, shipY, paint)

        Font5x5.draw(canvas, paint, "WARP IN -> SETOR $sectorLevel", virtualW / 2f, virtualH / 2f - 40f, 3, 0xFF38bdf8.toInt(), "center")
    }

    private fun renderPlaying(canvas: Canvas, now: Long) {
        paint.color = 0xFF334155.toInt()
        for (s in stars) {
            canvas.drawRect(s.x, s.y, s.x + s.size, s.y + s.size, paint)
        }

        val px = virtualW / 2f + shakeX
        val py = virtualH / 2f + shakeY

        for (p in platforms) {
            val pos = toScreen(p.x, p.y)
            paint.color = 0xFF1b3556.toInt()
            for (sr in p.subRects) {
                canvas.drawRect(pos.x + sr.ox - sr.w / 2f, pos.y + sr.oy - sr.h / 2f, pos.x + sr.ox + sr.w / 2f, pos.y + sr.oy + sr.h / 2f, paint)
            }
        }

        for (pt in platformTurrets) {
            val pos = toScreen(pt.x, pt.y)
            if (pos.x > -50f && pos.x < virtualW + 50f && pos.y > -50f && pos.y < virtualH + 50f) {
                canvas.drawBitmap(sprPlatBase, pos.x - sprPlatBase.width / 2f, pos.y - sprPlatBase.height / 2f, paint)
                if (pt.alive) {
                    canvas.drawBitmap(sprPlatGun, pos.x - sprPlatGun.width / 2f, pos.y - sprPlatGun.height / 2f, paint)
                }
            }
        }

        for (b in bases) {
            val sx = px + getDelta(playerX, b.x)
            val sy = py + getDelta(playerY, b.y)
            if (sx > -150f && sx < virtualW + 150f && sy > -150f && sy < virtualH + 150f) {
                val bSpr = stationSprites[b.shapeIdx]
                canvas.drawBitmap(bSpr, sx - bSpr.width / 2f, sy - bSpr.height / 2f, paint)

                for (pod in b.pods) {
                    val podSpr = if (pod.alive) sprTurretAlive else sprTurretDead
                    canvas.drawBitmap(podSpr, sx + pod.lx - podSpr.width / 2f, sy + pod.ly - podSpr.height / 2f, paint)
                }
            }
        }

        paint.color = 0xFF38bdf8.toInt()
        for (b in bullets) {
            val sx = px + getDelta(playerX, b.x)
            val sy = py + getDelta(playerY, b.y)
            canvas.drawRect(sx - 3f, sy - 3f, sx + 3f, sy + 3f, paint)
        }

        for (e in enemies) {
            val sx = px + getDelta(playerX, e.x)
            val sy = py + getDelta(playerY, e.y)
            if (sx > -50f && sx < virtualW + 50f && sy > -50f && sy < virtualH + 50f) {
                val spr = when (e.type) {
                    "striker" -> sprStriker
                    "gunship" -> sprGunship
                    else -> sprInterceptor
                }
                canvas.drawBitmap(spr, sx - spr.width / 2f, sy - spr.height / 2f, paint)
            }
        }

        for (eb in enemyBullets) {
            val sx = px + getDelta(playerX, eb.x)
            val sy = py + getDelta(playerY, eb.y)
            paint.color = eb.color
            canvas.drawRect(sx - 3f, sy - 3f, sx + 3f, sy + 3f, paint)
        }

        paint.color = 0xFFfde047.toInt()
        for (g in gems) {
            val sx = px + getDelta(playerX, g.x)
            val sy = py + getDelta(playerY, g.y)
            canvas.drawRect(sx - 4f, sy - 4f, sx + 4f, sy + 4f, paint)
        }

        canvas.save()
        canvas.translate(px, py)
        val pSpr = if (hurtFlash > 0) sprPlayerHitRed else sprPlayer
        canvas.drawBitmap(pSpr, -pSpr.width / 2f, -pSpr.height / 2f, paint)

        paint.color = if (isDashing) 0xFF38bdf8.toInt() else 0xFFf97316.toInt()
        val flameLen = if (isDashing) 28f else 12f
        canvas.drawRect(-9f - 2f, 12f, -9f + 2f, 12f + flameLen, paint)
        canvas.drawRect(9f - 2f, 12f, 9f + 2f, 12f + flameLen, paint)
        canvas.restore()

        if (hurtFlash > 0) {
            paint.color = 0x55ff2a55.toInt()
            canvas.drawRect(0f, 0f, virtualW, virtualH, paint)
        }

        renderHUD(canvas)
    }

    private fun renderHUD(canvas: Canvas) {
        drawPixelButton(canvas, 10f, 10f, 34f, 34f, "||", 0xFF1e293b.toInt(), 0xFF38bdf8.toInt(), 0xFFFFFFFF.toInt()) {
            screenState = ScreenState.PAUSED
            touchStart = null
            soundEngine.playSfx("click")
        }

        Font5x5.draw(canvas, paint, "BASES:${bases.size}", 50f, 11f, 2, 0xFFff2a55.toInt())
        Font5x5.draw(canvas, paint, "KILLS:$kills", 50f, 25f, 2, 0xFF38bdf8.toInt())

        // Mini Mapa / Radar
        val radSize = 82f
        val radX = virtualW - radSize - 10f
        val radY = 10f
        paint.color = 0xB302040a.toInt()
        canvas.drawRect(radX, radY, radX + radSize, radY + radSize, paint)
        paint.color = 0xFF22d3ee.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(radX, radY, radX + radSize, radY + radSize, paint)
        paint.style = Paint.Style.FILL

        val rcx = radX + radSize / 2f
        val rcy = radY + radSize / 2f
        paint.color = 0xFFffffff.toInt()
        canvas.drawRect(rcx - 2f, rcy - 2f, rcx + 2f, rcy + 2f, paint)

        for (b in bases) {
            val bx = rcx + (getDelta(playerX, b.x) / (worldSize * 0.44f)) * (radSize / 2f)
            val by = rcy + (getDelta(playerY, b.y) / (worldSize * 0.44f)) * (radSize / 2f)
            paint.color = 0xFFff2a55.toInt()
            canvas.drawRect(bx - 3f, by - 3f, bx + 3f, by + 3f, paint)
        }

        // XP Text e Barra limpa sem sobreposição
        val xpLabel = "LVL $playerLevel • XP $playerXp/$playerNextXp"
        Font5x5.draw(canvas, paint, xpLabel, virtualW / 2f - 20f, 10f, 2, 0xFFfde047.toInt(), "center")
        val xpW = 180f
        val xpX = virtualW / 2f - 110f
        paint.color = 0xFF090d16.toInt()
        canvas.drawRect(xpX, 26f, xpX + xpW, 34f, paint)
        paint.color = 0xFF38bdf8.toInt()
        val fillW = xpW * (playerXp.toFloat() / playerNextXp).coerceIn(0f, 1f)
        canvas.drawRect(xpX, 26f, xpX + fillW, 34f, paint)

        // Barra de Blindagem
        val hpW = min(virtualW - 32f, 540f)
        val hpX = virtualW / 2f - hpW / 2f
        val hpY = virtualH - 50f
        Font5x5.draw(canvas, paint, "BLINDAGEM ${playerHp.toInt()}/${playerMaxHp.toInt()}", virtualW / 2f, hpY - 14f, 2, 0xFFcbd5e1.toInt(), "center")
        paint.color = 0xFF090d16.toInt()
        canvas.drawRect(hpX, hpY, hpX + hpW, hpY + 16f, paint)
        paint.color = if (playerHp > 30f) 0xFFef4444.toInt() else 0xFFfde047.toInt()
        val hpFill = hpW * (playerHp / playerMaxHp).coerceIn(0f, 1f)
        canvas.drawRect(hpX + 2f, hpY + 2f, hpX + hpFill, hpY + 14f, paint)

        // Botão Touch HIPER
        val btnW = 84f
        val btnH = 34f
        val btnX = virtualW - btnW - 14f
        val btnY = hpY - btnH - 10f
        val dashReady = dashCooldown <= 0
        drawPixelButton(
            canvas, btnX, btnY, btnW, btnH,
            if (dashReady) "HIPER" else "${((1f - dashCooldown.toFloat() / 70f) * 100).toInt()}%",
            if (dashReady) 0xFF083344.toInt() else 0xFF090d16.toInt(),
            0xFF38bdf8.toInt(),
            if (dashReady) 0xFFffffff.toInt() else 0xFF64748b.toInt()
        ) {
            triggerHyperVelocity()
        }

        touchStart?.let { ts ->
            touchCurrent?.let { tc ->
                paint.color = 0x5538bdf8.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRect(ts.x - 44f, ts.y - 44f, ts.x + 44f, ts.y + 44f, paint)
                paint.style = Paint.Style.FILL

                val dx = tc.x - ts.x
                val dy = tc.y - ts.y
                val dist = min(hypot(dx, dy), 44f)
                val ang = atan2(dy, dx)
                val kx = ts.x + cos(ang) * dist
                val ky = ts.y + sin(ang) * dist

                paint.color = 0xFF38bdf8.toInt()
                canvas.drawRect(kx - 16f, ky - 16f, kx + 16f, ky + 16f, paint)
            }
        }
    }

    private fun renderPaused(canvas: Canvas) {
        Font5x5.draw(canvas, paint, "JOGO PAUSADO", virtualW / 2f, 165f, 4, 0xFFfde047.toInt(), "center")
        val btnW = 280f
        val bx = virtualW / 2f - btnW / 2f

        drawPixelButton(canvas, bx, 270f, btnW, 50f, "CONTINUAR", 0xFF10b981.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            screenState = ScreenState.PLAYING
            soundEngine.playSfx("click")
        }
        drawPixelButton(canvas, bx, 340f, btnW, 46f, "REINICIAR", 0xFF1e293b.toInt(), 0xFFfde047.toInt(), 0xFFfde047.toInt()) {
            startNewGame(false)
            soundEngine.playSfx("click")
        }
        drawPixelButton(canvas, bx, 410f, btnW, 46f, "SAIR PARA MENU", 0xFF881337.toInt(), 0xFFff2a55.toInt(), 0xFFFFFFFF.toInt()) {
            screenState = ScreenState.MENU
            soundEngine.playSfx("click")
        }
    }

    private fun renderGameOver(canvas: Canvas) {
        Font5x5.draw(canvas, paint, "GAME OVER", virtualW / 2f, 160f, 5, 0xFFff2a55.toInt(), "center")
        Font5x5.draw(canvas, paint, "KILLS: $kills", virtualW / 2f, 230f, 3, 0xFF38bdf8.toInt(), "center")
        val btnW = 280f
        val bx = virtualW / 2f - btnW / 2f

        drawPixelButton(canvas, bx, 320f, btnW, 50f, "JOGAR NOVAMENTE", 0xFF10b981.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            startNewGame(false)
        }
        drawPixelButton(canvas, bx, 390f, btnW, 46f, "MENU INICIAL", 0xFF1e293b.toInt(), 0xFF38bdf8.toInt(), 0xFF38bdf8.toInt()) {
            screenState = ScreenState.MENU
        }
    }

    private fun renderSettings(canvas: Canvas) {
        Font5x5.draw(canvas, paint, "CONFIGURACOES", virtualW / 2f, 70f, 4, 0xFFfde047.toInt(), "center")
        val btnW = 280f
        val bx = virtualW / 2f - btnW / 2f

        drawPixelButton(canvas, bx, 200f, btnW, 48f, "TREMOR: ${if (enableShake) "LIGADO" else "DESL"}", 0xFF1e293b.toInt(), 0xFF38bdf8.toInt(), 0xFFFFFFFF.toInt()) {
            enableShake = !enableShake
        }
        drawPixelButton(canvas, bx, 270f, btnW, 48f, "TELA CRT: ${if (enableCrt) "LIGADO" else "DESL"}", 0xFF1e293b.toInt(), 0xFF38bdf8.toInt(), 0xFFFFFFFF.toInt()) {
            enableCrt = !enableCrt
        }
        drawPixelButton(canvas, bx, 360f, btnW, 48f, "VOLTAR", 0xFF10b981.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            screenState = previousState
        }
    }

    private fun renderHowTo(canvas: Canvas) {
        Font5x5.draw(canvas, paint, "COMO JOGAR", virtualW / 2f, 70f, 4, 0xFFfde047.toInt(), "center")
        Font5x5.draw(canvas, paint, "ARRASTE O DEDO NA TELA", virtualW / 2f, 140f, 2, 0xFFFFFFFF.toInt(), "center")
        Font5x5.draw(canvas, paint, "PARA PILOTAR SUA NAVE.", virtualW / 2f, 165f, 2, 0xFF38bdf8.toInt(), "center")
        Font5x5.draw(canvas, paint, "O TIRO E AUTOMATICO!", virtualW / 2f, 205f, 2, 0xFFfde047.toInt(), "center")
        Font5x5.draw(canvas, paint, "DESTRUA OS REATORES DAS BASES", virtualW / 2f, 245f, 2, 0xFFff2a55.toInt(), "center")
        Font5x5.draw(canvas, paint, "USE O BOTAO HIPER PARA DESVIAR", virtualW / 2f, 285f, 2, 0xFF10b981.toInt(), "center")

        val btnW = 280f
        val bx = virtualW / 2f - btnW / 2f
        drawPixelButton(canvas, bx, 380f, btnW, 48f, "VOLTAR AO MENU", 0xFF10b981.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()) {
            screenState = ScreenState.MENU
        }
    }

    private fun drawPixelButton(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        label: String, bgColor: Int, borderColor: Int, textColor: Int,
        onClick: () -> Unit
    ) {
        paint.color = 0xFF000000.toInt()
        canvas.drawRect(x + 3f, y + 3f, x + w + 3f, y + h + 3f, paint)

        paint.color = bgColor
        canvas.drawRect(x, y, x + w, y + h, paint)

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(x, y, x + w, y + h, paint)
        paint.style = Paint.Style.FILL

        var tScale = 3
        if (Font5x5.measure(label, 3) > w - 14f) tScale = 2
        if (Font5x5.measure(label, 2) > w - 10f) tScale = 1

        val textY = y + floor((h - 5 * tScale) / 2f)
        Font5x5.draw(canvas, paint, label, x + w / 2f, textY, tScale, textColor, "center")

        uiButtons.add(UiButton(x, y, w, h, onClick))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x / scale
        val touchY = event.y / scale

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (b in uiButtons) {
                    if (touchX >= b.x && touchX <= b.x + b.w && touchY >= b.y && touchY <= b.y + b.h) {
                        b.onClick()
                        return true
                    }
                }

                if (screenState == ScreenState.PLAYING) {
                    touchStart = PointF(touchX, touchY)
                    touchCurrent = PointF(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (screenState == ScreenState.PLAYING && touchStart != null) {
                    touchCurrent = PointF(touchX, touchY)
                    val dx = touchX - touchStart!!.x
                    val dy = touchY - touchStart!!.y
                    if (hypot(dx, dy) > 6f) {
                        val ang = atan2(dy, dx)
                        playerAngle = ang
                        moveDir = PointF(cos(ang), sin(ang))
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchStart = null
                touchCurrent = null
            }
        }
        return true
    }

    fun handleBackPressed(): Boolean {
        return when (screenState) {
            ScreenState.PLAYING -> {
                screenState = ScreenState.PAUSED
                true
            }
            ScreenState.PAUSED, ScreenState.SETTINGS, ScreenState.HOWTO -> {
                screenState = ScreenState.MENU
                true
            }
            else -> false
        }
    }
}
