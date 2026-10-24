package com.mercador.core.flight

import kotlin.math.sqrt

/** Vetor 3D mutável (o voo roda a 60 passos por segundo: nada de alocar por passo). */
class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    constructor(v: Vec3) : this(v.x, v.y, v.z)

    fun set(x: Float, y: Float, z: Float): Vec3 { this.x = x; this.y = y; this.z = z; return this }
    fun set(v: Vec3): Vec3 = set(v.x, v.y, v.z)
    fun add(v: Vec3): Vec3 = set(x + v.x, y + v.y, z + v.z)
    fun add(dx: Float, dy: Float, dz: Float): Vec3 = set(x + dx, y + dy, z + dz)
    fun sub(v: Vec3): Vec3 = set(x - v.x, y - v.y, z - v.z)
    fun scl(s: Float): Vec3 = set(x * s, y * s, z * s)
    /** this += v·s */
    fun mulAdd(v: Vec3, s: Float): Vec3 = set(x + v.x * s, y + v.y * s, z + v.z * s)
    fun dot(v: Vec3): Float = x * v.x + y * v.y + z * v.z
    fun len2(): Float = x * x + y * y + z * z
    fun len(): Float = sqrt(len2())
    fun nor(): Vec3 { val l = len(); return if (l > 1e-9f) scl(1f / l) else this }
    /** this = this × v */
    fun crs(v: Vec3): Vec3 = set(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    override fun toString() = "($x, $y, $z)"

    companion object {
        val Y = Vec3(0f, 1f, 0f)
    }
}
