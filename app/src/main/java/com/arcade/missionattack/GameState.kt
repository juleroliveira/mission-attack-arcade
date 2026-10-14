package com.arcade.missionattack

data class UiButton(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val onClick: () -> Unit
)

data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val color: Int
)

data class WarpStar(
    var x: Float,
    var y: Float,
    val speed: Float,
    val width: Float,
    val baseLen: Float,
    val color: Int
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var ang: Float,
    var range: Float,
    var damage: Float,
    var size: Float,
    var isLaser: Boolean = false,
    var pierce: Int = 1
)

data class EnemyBullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var ang: Float,
    var range: Float,
    var damage: Float,
    var color: Int
)

data class Gem(
    var x: Float,
    var y: Float,
    val xp: Int,
    val seed: Int
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Int,
    var alpha: Float = 1.0f,
    val decay: Float = 0.04f
)

data class Pod(
    val index: Int,
    val lx: Float,
    val ly: Float,
    val localAng: Float,
    var hp: Float,
    val maxHp: Float,
    var alive: Boolean = true,
    var hitFlash: Int = 0,
    var lastShot: Long = 0
)

data class BaseStation(
    var x: Float,
    var y: Float,
    val shapeIdx: Int,
    val name: String,
    val r: Float,
    var coreHp: Float,
    val coreMaxHp: Float,
    val coreR: Float,
    var coreFlash: Int = 0,
    var angle: Float = 0f,
    val pods: MutableList<Pod>,
    var dying: Boolean = false,
    var dyingTimer: Int = 0
)

data class PlatformTurret(
    var x: Float,
    var y: Float,
    var hp: Float,
    val maxHp: Float,
    var alive: Boolean = true,
    var angle: Float = 0f,
    var hitFlash: Int = 0,
    var lastShot: Long = 0
)

data class PlatformRect(
    val ox: Float,
    val oy: Float,
    val w: Float,
    val h: Float
)

data class StingerPlatform(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val shape: String,
    val subRects: List<PlatformRect>
)

data class Enemy(
    val type: String,
    var x: Float,
    var y: Float,
    var angle: Float,
    var speed: Float,
    var hp: Float,
    val maxHp: Float,
    val r: Float,
    val color: Int,
    val xp: Int,
    val shotDelay: Long,
    var lastShot: Long = 0,
    var hitFlash: Int = 0,
    var isCharging: Boolean = false,
    var chargeAngle: Float = 0f
)

enum class ScreenState {
    MENU,
    WARP_IN,
    PLAYING,
    PAUSED,
    GAMEOVER,
    SETTINGS,
    HOWTO
}
