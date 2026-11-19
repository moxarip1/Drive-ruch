package com.example.sweetrush.render3d

import android.opengl.GLES30
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.track.DecoType
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 13-float vertex layout (52 bytes/vertex):
 *   loc 0: position (x, y, z)
 *   loc 1: normal   (nx, ny, nz)
 *   loc 2: color    (r, g, b, a)
 *   loc 3: texInfo  (u, v, matType)
 */
class GpuMesh(
    vertices: FloatArray,
    val vertexCount: Int
) {
    private val vao: Int
    private val vbo: Int

    init {
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        vao = vaos[0]
        vbo = vbos[0]

        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buf.put(vertices)
        buf.position(0)

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_STATIC_DRAW)

        val stride = 13 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, stride, 10 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        if (vertexCount <= 0) return
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
    }
}

object CandyMaterials {
    const val MAT_CANDY = 0f
    const val MAT_ROAD = 1f
    const val MAT_TIRE = 2f
    const val MAT_CHROME = 3f
    const val MAT_GUMMY = 4f
    const val MAT_EMISSIVE = 5f
    const val MAT_LIQUID = 6f
    const val MAT_CARAMEL = 7f
    const val MAT_WAFER = 8f
    const val MAT_MARSHMALLOW = 9f
}

class MeshAccumulator {
    private val data = ArrayList<Float>(8192)

    fun addTri(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f,
        u1: Float = 0f, v1: Float = 0f,
        u2: Float = 1f, v2: Float = 0f,
        u3: Float = 0.5f, v3: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
        val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len > 1e-6f) {
            nx /= len; ny /= len; nz /= len
        } else {
            nx = 0f; ny = 1f; nz = 0f
        }
        pushV(x1, y1, z1, nx, ny, nz, r, g, b, a, u1, v1, matType)
        pushV(x2, y2, z2, nx, ny, nz, r, g, b, a, u2, v2, matType)
        pushV(x3, y3, z3, nx, ny, nz, r, g, b, a, u3, v3, matType)
    }

    fun addTriWithNormals(
        x1: Float, y1: Float, z1: Float, nx1: Float, ny1: Float, nz1: Float,
        x2: Float, y2: Float, z2: Float, nx2: Float, ny2: Float, nz2: Float,
        x3: Float, y3: Float, z3: Float, nx3: Float, ny3: Float, nz3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f,
        u1: Float = 0f, v1: Float = 0f,
        u2: Float = 1f, v2: Float = 0f,
        u3: Float = 0.5f, v3: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        pushV(x1, y1, z1, nx1, ny1, nz1, r, g, b, a, u1, v1, matType)
        pushV(x2, y2, z2, nx2, ny2, nz2, r, g, b, a, u2, v2, matType)
        pushV(x3, y3, z3, nx3, ny3, nz3, r, g, b, a, u3, v3, matType)
    }

    private fun pushV(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float,
        u: Float, v: Float, matType: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b); data.add(a)
        data.add(u); data.add(v); data.add(matType)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float = 1f,
        u0: Float = 0f, v0: Float = 0f,
        u1: Float = 1f, v1: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        addTri(x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a, u0, v0, u1, v0, u1, v1, matType)
        addTri(x1, y1, z1, x3, y3, z3, x4, y4, z4, r, g, b, a, u0, v0, u1, v1, u0, v1, matType)
    }

    fun addQuadSmooth(
        p1: Vec3, n1: Vec3, r1: Float, g1: Float, b1: Float,
        p2: Vec3, n2: Vec3, r2: Float, g2: Float, b2: Float,
        p3: Vec3, n3: Vec3, r3: Float, g3: Float, b3: Float,
        p4: Vec3, n4: Vec3, r4: Float, g4: Float, b4: Float,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        pushV(p1.x, p1.y, p1.z, n1.x, n1.y, n1.z, r1, g1, b1, 1f, 0f, 0f, matType)
        pushV(p2.x, p2.y, p2.z, n2.x, n2.y, n2.z, r2, g2, b2, 1f, 1f, 0f, matType)
        pushV(p3.x, p3.y, p3.z, n3.x, n3.y, n3.z, r3, g3, b3, 1f, 1f, 1f, matType)

        pushV(p1.x, p1.y, p1.z, n1.x, n1.y, n1.z, r1, g1, b1, 1f, 0f, 0f, matType)
        pushV(p3.x, p3.y, p3.z, n3.x, n3.y, n3.z, r3, g3, b3, 1f, 1f, 1f, matType)
        pushV(p4.x, p4.y, p4.z, n4.x, n4.y, n4.z, r4, g4, b4, 1f, 0f, 1f, matType)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        hex: Long, a: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY,
        uTag: Float = 0.5f
    ) {
        val (r, g, b) = hexToRgb(hex)
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a, uTag, 0f, uTag, 1f, matType)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a, uTag, 0f, uTag, 1f, matType)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a, uTag, 0f, uTag, 1f, matType)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a, uTag, 0f, uTag, 1f, matType)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a, uTag, 0f, uTag, 1f, matType)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a, uTag, 0f, uTag, 1f, matType)
    }

    /**
     * Beveled / chamfered prism for sleek sculpted mechanical & automotive surfaces.
     */
    fun addBeveledBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        topTaperX: Float = 0.88f,
        topTaperZ: Float = 0.90f,
        hex: Long,
        topHex: Long = hex,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val (r, g, b) = hexToRgb(hex)
        val (tr, tg, tb) = hexToRgb(topHex)
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val tx = hx * topTaperX; val tz = hz * topTaperZ
        val y0 = cy - hy; val y1 = cy + hy

        // Bottom vertices
        val b00 = Vec3(cx - hx, y0, cz - hz)
        val b10 = Vec3(cx + hx, y0, cz - hz)
        val b11 = Vec3(cx + hx, y0, cz + hz)
        val b01 = Vec3(cx - hx, y0, cz + hz)
        // Top beveled vertices
        val t00 = Vec3(cx - tx, y1, cz - tz)
        val t10 = Vec3(cx + tx, y1, cz - tz)
        val t11 = Vec3(cx + tx, y1, cz + tz)
        val t01 = Vec3(cx - tx, y1, cz + tz)

        addQuad(b01.x, b01.y, b01.z, b11.x, b11.y, b11.z, t11.x, t11.y, t11.z, t01.x, t01.y, t01.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(b10.x, b10.y, b10.z, b00.x, b00.y, b00.z, t00.x, t00.y, t00.z, t10.x, t10.y, t10.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(b00.x, b00.y, b00.z, b01.x, b01.y, b01.z, t01.x, t01.y, t01.z, t00.x, t00.y, t00.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(b11.x, b11.y, b11.z, b10.x, b10.y, b10.z, t10.x, t10.y, t10.z, t11.x, t11.y, t11.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(t01.x, t01.y, t01.z, t11.x, t11.y, t11.z, t10.x, t10.y, t10.z, t00.x, t00.y, t00.z, tr, tg, tb, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(b00.x, b00.y, b00.z, b10.x, b10.y, b10.z, b11.x, b11.y, b11.z, b01.x, b01.y, b01.z, r * 0.7f, g * 0.7f, b * 0.7f, 1f, 0f, 0f, 1f, 1f, matType)
    }

    /**
     * Strut/pipe connecting any two arbitrary 3D points (great for A-arms, suspension, roll bars, steering columns).
     */
    fun addStrut(
        p0: Vec3,
        p1: Vec3,
        radius: Float,
        segs: Int = 6,
        hex: Long,
        matType: Float = CandyMaterials.MAT_CHROME
    ) {
        val dir = p1 - p0
        val len = dir.length()
        if (len < 1e-4f) return
        val fwd = dir * (1f / len)
        val ref = if (kotlin.math.abs(fwd.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
        // Cross product fwd x ref
        val rx = fwd.y * ref.z - fwd.z * ref.y
        val ry = fwd.z * ref.x - fwd.x * ref.z
        val rz = fwd.x * ref.y - fwd.y * ref.x
        val right = Vec3(rx, ry, rz).normalized()
        val ux = right.y * fwd.z - right.z * fwd.y
        val uy = right.z * fwd.x - right.x * fwd.z
        val uz = right.x * fwd.y - right.y * fwd.x
        val up = Vec3(ux, uy, uz).normalized()

        val (r, g, b) = hexToRgb(hex)
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val off0 = right * (cos(a0) * radius) + up * (sin(a0) * radius)
            val off1 = right * (cos(a1) * radius) + up * (sin(a1) * radius)
            val s0 = p0 + off0
            val s1 = p0 + off1
            val e0 = p1 + off0
            val e1 = p1 + off1
            addQuad(s0.x, s0.y, s0.z, s1.x, s1.y, s1.z, e1.x, e1.y, e1.z, e0.x, e0.y, e0.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
        }
    }

    /**
     * Multi-station Z-lofted sculpted hull: each station is (z, halfWidth, yBottom, yTop, colorHex).
     * Creates curved, aerodynamic race car bodies with nose tapers, flared fenders, and rear decks!
     */
    data class LoftStation(
        val z: Float,
        val halfWidth: Float,
        val yBottom: Float,
        val yTop: Float,
        val hex: Long,
        val topHex: Long = hex
    )

    fun addLoftedHull(
        stations: List<LoftStation>,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        if (stations.size < 2) return
        for (i in 0 until stations.size - 1) {
            val s0 = stations[i]
            val s1 = stations[i + 1]
            val (r0, g0, b0) = hexToRgb(s0.hex)
            val (tr0, tg0, tb0) = hexToRgb(s0.topHex)
            val (tr1, tg1, tb1) = hexToRgb(s1.topHex)

            val w0 = s0.halfWidth; val w1 = s1.halfWidth
            val wt0 = w0 * 0.78f; val wt1 = w1 * 0.78f
            val yM0 = s0.yBottom + (s0.yTop - s0.yBottom) * 0.58f
            val yM1 = s1.yBottom + (s1.yTop - s1.yBottom) * 0.58f

            // Bottom belly
            addQuad(-w0, s0.yBottom, s0.z, w0, s0.yBottom, s0.z, w1, s1.yBottom, s1.z, -w1, s1.yBottom, s1.z, r0 * 0.65f, g0 * 0.65f, b0 * 0.65f, 1f, 0f, 0f, 1f, 1f, matType)
            // Lower side skirts (Left & Right)
            addQuad(-w0, s0.yBottom, s0.z, -w0, yM0, s0.z, -w1, yM1, s1.z, -w1, s1.yBottom, s1.z, r0, g0, b0, 1f, 0f, 0f, 1f, 1f, matType)
            addQuad(w0, yM0, s0.z, w0, s0.yBottom, s0.z, w1, s1.yBottom, s1.z, w1, yM1, s1.z, r0, g0, b0, 1f, 0f, 0f, 1f, 1f, matType)
            // Upper chamfered shoulders (Left & Right)
            addQuad(-w0, yM0, s0.z, -wt0, s0.yTop, s0.z, -wt1, s1.yTop, s1.z, -w1, yM1, s1.z, tr0, tg0, tb0, 1f, 0f, 0f, 1f, 1f, matType)
            addQuad(wt0, s0.yTop, s0.z, w0, yM0, s0.z, w1, yM1, s1.z, wt1, s1.yTop, s1.z, tr1, tg1, tb1, 1f, 0f, 0f, 1f, 1f, matType)
            // Top sculpted deck
            addQuad(-wt0, s0.yTop, s0.z, wt0, s0.yTop, s0.z, wt1, s1.yTop, s1.z, -wt1, s1.yTop, s1.z, (tr0 + tr1) * 0.5f, (tg0 + tg1) * 0.5f, (tb0 + tb1) * 0.5f, 1f, 0f, 0f, 1f, 1f, matType)
        }
        // Cap front and rear ends
        val front = stations.last()
        val (fr, fg, fb) = hexToRgb(front.hex)
        val fw = front.halfWidth; val fwt = fw * 0.78f; val fyM = front.yBottom + (front.yTop - front.yBottom) * 0.58f
        addQuad(-fw, front.yBottom, front.z, fw, front.yBottom, front.z, fw, fyM, front.z, -fw, fyM, front.z, fr, fg, fb, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(-fw, fyM, front.z, fw, fyM, front.z, fwt, front.yTop, front.z, -fwt, front.yTop, front.z, fr, fg, fb, 1f, 0f, 0f, 1f, 1f, matType)

        val back = stations.first()
        val (br, bg, bb) = hexToRgb(back.hex)
        val bw = back.halfWidth; val bwt = bw * 0.78f; val byM = back.yBottom + (back.yTop - back.yBottom) * 0.58f
        addQuad(bw, back.yBottom, back.z, -bw, back.yBottom, back.z, -bw, byM, back.z, bw, byM, back.z, br, bg, bb, 1f, 0f, 0f, 1f, 1f, matType)
        addQuad(bw, byM, back.z, -bw, byM, back.z, -bwt, back.yTop, back.z, bwt, back.yTop, back.z, br, bg, bb, 1f, 0f, 0f, 1f, 1f, matType)
    }

    fun addCylinder(
        cx: Float, cy: Float, cz: Float,
        radBot: Float, radTop: Float, height: Float,
        segs: Int, hex: Long, altHex: Long = hex, a: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val (r1, g1, b1) = hexToRgb(hex)
        val (r2, g2, b2) = hexToRgb(altHex)
        val y0 = cy
        val y1 = cy + height
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val (r, g, b) = if (i % 2 == 0) Triple(r1, g1, b1) else Triple(r2, g2, b2)

            val bx0 = cx + c0 * radBot; val bz0 = cz + s0 * radBot
            val bx1 = cx + c1 * radBot; val bz1 = cz + s1 * radBot
            val tx0 = cx + c0 * radTop; val tz0 = cz + s0 * radTop
            val tx1 = cx + c1 * radTop; val tz1 = cz + s1 * radTop

            addQuad(bx0, y0, bz0, bx1, y0, bz1, tx1, y1, tz1, tx0, y1, tz0, r, g, b, a, 0f, 0f, 1f, 1f, matType)
            if (radTop > 0.001f) {
                addTri(cx, y1, cz, tx0, y1, tz0, tx1, y1, tz1, r, g, b, a, 0f, 0f, 1f, 0f, 0.5f, 1f, matType)
            }
            if (radBot > 0.001f) {
                addTri(cx, y0, cz, bx1, y0, bz1, bx0, y0, bz0, r, g, b, a, 0f, 0f, 1f, 0f, 0.5f, 1f, matType)
            }
        }
    }

    fun addHorizontalCylinderZ(
        cx: Float, cy: Float, cz: Float,
        radBack: Float, radFront: Float, length: Float,
        segs: Int, hex: Long, altHex: Long = hex,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val (r1, g1, b1) = hexToRgb(hex)
        val (r2, g2, b2) = hexToRgb(altHex)
        val z0 = cz - length * 0.5f
        val z1 = cz + length * 0.5f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val (r, g, b) = if (i % 2 == 0) Triple(r1, g1, b1) else Triple(r2, g2, b2)
            val bx0 = cx + cos(a0) * radBack; val by0 = cy + sin(a0) * radBack
            val bx1 = cx + cos(a1) * radBack; val by1 = cy + sin(a1) * radBack
            val fx0 = cx + cos(a0) * radFront; val fy0 = cy + sin(a0) * radFront
            val fx1 = cx + cos(a1) * radFront; val fy1 = cy + sin(a1) * radFront
            addQuad(bx0, by0, z0, bx1, by1, z0, fx1, fy1, z1, fx0, fy0, z1, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
            if (radFront > 0.001f) addTri(cx, cy, z1, fx0, fy0, z1, fx1, fy1, z1, r, g, b, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, matType)
            if (radBack > 0.001f) addTri(cx, cy, z0, bx1, by1, z0, bx0, by0, z0, r, g, b, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, matType)
        }
    }

    fun addSphere(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        stacks: Int, slices: Int,
        hex: Long, altHex: Long = hex, a: Float = 1f,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val (r1, g1, b1) = hexToRgb(hex)
        val (r2, g2, b2) = hexToRgb(altHex)
        for (i in 0 until stacks) {
            val phi0 = (i.toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            val phi1 = ((i + 1).toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            for (j in 0 until slices) {
                val th0 = (j.toFloat() / slices) * (2f * PI.toFloat())
                val th1 = ((j + 1).toFloat() / slices) * (2f * PI.toFloat())
                val (r, g, b) = if ((i + j) % 2 == 0) Triple(r1, g1, b1) else Triple(r2, g2, b2)

                val p00 = spherePt(cx, cy, cz, rx, ry, rz, phi0, th0)
                val p10 = spherePt(cx, cy, cz, rx, ry, rz, phi0, th1)
                val p11 = spherePt(cx, cy, cz, rx, ry, rz, phi1, th1)
                val p01 = spherePt(cx, cy, cz, rx, ry, rz, phi1, th0)

                addQuad(p00.x, p00.y, p00.z, p10.x, p10.y, p10.z, p11.x, p11.y, p11.z, p01.x, p01.y, p01.z, r, g, b, a, 0f, 0f, 1f, 1f, matType)
            }
        }
    }

    fun addTorus(
        cx: Float, cy: Float, cz: Float,
        majorR: Float, minorR: Float,
        majorSegs: Int, minorSegs: Int,
        hex: Long, topGlazeHex: Long = hex,
        verticalArch: Boolean = false,
        matType: Float = CandyMaterials.MAT_CANDY
    ) {
        val (r1, g1, b1) = hexToRgb(hex)
        val (r2, g2, b2) = hexToRgb(topGlazeHex)
        for (i in 0 until majorSegs) {
            val u0 = (i.toFloat() / majorSegs) * (2f * PI.toFloat())
            val u1 = ((i + 1).toFloat() / majorSegs) * (2f * PI.toFloat())
            for (j in 0 until minorSegs) {
                val v0 = (j.toFloat() / minorSegs) * (2f * PI.toFloat())
                val v1 = ((j + 1).toFloat() / minorSegs) * (2f * PI.toFloat())
                val p00 = torusPt(cx, cy, cz, majorR, minorR, u0, v0, verticalArch)
                val p10 = torusPt(cx, cy, cz, majorR, minorR, u1, v0, verticalArch)
                val p11 = torusPt(cx, cy, cz, majorR, minorR, u1, v1, verticalArch)
                val p01 = torusPt(cx, cy, cz, majorR, minorR, u0, v1, verticalArch)
                val isTop = sin(v0) > 0.1f
                val (r, g, b) = if (isTop) Triple(r2, g2, b2) else Triple(r1, g1, b1)
                addQuad(p00.x, p00.y, p00.z, p10.x, p10.y, p10.z, p11.x, p11.y, p11.z, p01.x, p01.y, p01.z, r, g, b, 1f, 0f, 0f, 1f, 1f, matType)
            }
        }
    }

    fun addShadowOval(rx: Float, rz: Float, segs: Int = 16, alpha: Float = 0.42f) {
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val x0 = cos(a0) * rx; val z0 = sin(a0) * rz
            val x1 = cos(a1) * rx; val z1 = sin(a1) * rz
            addTri(0f, 0.02f, 0f, x1, 0.02f, z1, x0, 0.02f, z0, 0.08f, 0.03f, 0.12f, alpha, 0.5f, 0.5f, 0f, 0f, 1f, 0f, CandyMaterials.MAT_CANDY)
        }
    }

    private fun torusPt(
        cx: Float, cy: Float, cz: Float,
        R: Float, r: Float, u: Float, v: Float, verticalArch: Boolean
    ): Vec3 {
        val ring = R + r * cos(v)
        return if (!verticalArch) {
            Vec3(cx + ring * cos(u), cy + r * sin(v), cz + ring * sin(u))
        } else {
            Vec3(cx + ring * cos(u), cy + ring * sin(u), cz + r * sin(v))
        }
    }

    private fun spherePt(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float, phi: Float, theta: Float
    ): Vec3 {
        val cPhi = cos(phi)
        return Vec3(
            cx + rx * cPhi * cos(theta),
            cy + ry * sin(phi),
            cz + rz * cPhi * sin(theta)
        )
    }

    fun build(): GpuMesh {
        val arr = FloatArray(data.size)
        for (i in data.indices) arr[i] = data[i]
        return GpuMesh(arr, data.size / 13)
    }

    companion object {
        fun hexToRgb(hex: Long): Triple<Float, Float, Float> {
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            return Triple(r, g, b)
        }
    }
}

object MeshFactory {

    fun buildTrackMesh(track: GeneratedTrack): GpuMesh {
        val acc = MeshAccumulator()
        val spec = CatalogData.trackById(track.id)
        val n = track.nodes.size
        val (rr, rg, rb) = MeshAccumulator.hexToRgb(spec.roadPrimaryHex)
        val (sr, sg, sb) = MeshAccumulator.hexToRgb(spec.roadStripeHex)
        val (c1r, c1g, c1b) = MeshAccumulator.hexToRgb(spec.curbPrimaryHex)
        val (c2r, c2g, c2b) = MeshAccumulator.hexToRgb(spec.curbSecondaryHex)

        for (i in 0 until n) {
            val n0 = track.nodes[i]
            val n1 = track.nodes[(i + 1) % n]
            val hw0 = n0.halfWidth
            val hw1 = n1.halfWidth

            val l0 = n0.center - n0.right * hw0
            val r0 = n0.center + n0.right * hw0
            val l1 = n1.center - n1.right * hw1
            val r1 = n1.center + n1.right * hw1

            if (i == 0 || i == 1) {
                val (fr, fg, fb) = if (i == 0) Triple(1f, 1f, 1f) else Triple(0.15f, 0.08f, 0.20f)
                acc.addQuad(l0.x, l0.y + 0.03f, l0.z, r0.x, r0.y + 0.03f, r0.z, r1.x, r1.y + 0.03f, r1.z, l1.x, l1.y + 0.03f, l1.z, fr, fg, fb, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD)
            } else {
                val mL0 = n0.center - n0.right * (hw0 * 0.14f)
                val mR0 = n0.center + n0.right * (hw0 * 0.14f)
                val mL1 = n1.center - n1.right * (hw1 * 0.14f)
                val mR1 = n1.center + n1.right * (hw1 * 0.14f)

                val shade = if (i % 2 == 0) 1.0f else 0.94f
                acc.addQuad(l0.x, l0.y, l0.z, mL0.x, mL0.y, mL0.z, mL1.x, mL1.y, mL1.z, l1.x, l1.y, l1.z, rr * shade, rg * shade, rb * shade, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD)
                acc.addQuad(mR0.x, mR0.y, mR0.z, r0.x, r0.y, r0.z, r1.x, r1.y, r1.z, mR1.x, mR1.y, mR1.z, rr * shade, rg * shade, rb * shade, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD)

                val stripeAlt = if ((i / 2) % 2 == 0) 1f else 0.78f
                acc.addQuad(mL0.x, mL0.y + 0.015f, mL0.z, mR0.x, mR0.y + 0.015f, mR0.z, mR1.x, mR1.y + 0.015f, mR1.z, mL1.x, mL1.y + 0.015f, mL1.z, sr * stripeAlt, sg * stripeAlt, sb * stripeAlt, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_CANDY)
            }

            val isAlt = (i % 2 == 0)
            val cr = if (isAlt) c1r else c2r
            val cg = if (isAlt) c1g else c2g
            val cb = if (isAlt) c1b else c2b
            val curbW = 1.25f
            val curbH = 0.52f

            val lcOut0 = l0 - n0.right * curbW
            val lcOut1 = l1 - n1.right * curbW
            acc.addQuad(l0.x, l0.y + curbH, l0.z, l0.x, l0.y, l0.z, l1.x, l1.y, l1.z, l1.x, l1.y + curbH, l1.z, cr, cg, cb)
            acc.addQuad(lcOut0.x, lcOut0.y + curbH, lcOut0.z, l0.x, l0.y + curbH, l0.z, l1.x, l1.y + curbH, l1.z, lcOut1.x, lcOut1.y + curbH, lcOut1.z, cr, cg, cb)

            val rcOut0 = r0 + n0.right * curbW
            val rcOut1 = r1 + n1.right * curbW
            acc.addQuad(r0.x, r0.y, r0.z, r0.x, r0.y + curbH, r0.z, r1.x, r1.y + curbH, r1.z, r1.x, r1.y, r1.z, cr, cg, cb)
            acc.addQuad(r0.x, r0.y + curbH, r0.z, rcOut0.x, rcOut0.y + curbH, rcOut0.z, rcOut1.x, rcOut1.y + curbH, rcOut1.z, r1.x, r1.y + curbH, r1.z, cr, cg, cb)

            if (n0.center.y > 0.3f || n1.center.y > 0.3f) {
                acc.addQuad(lcOut0.x, lcOut0.y + curbH, lcOut0.z, lcOut1.x, lcOut1.y + curbH, lcOut1.z, lcOut1.x, 0f, lcOut1.z, lcOut0.x, 0f, lcOut0.z, 0.72f, 0.45f, 0.25f)
                acc.addQuad(rcOut1.x, rcOut1.y + curbH, rcOut1.z, rcOut0.x, rcOut0.y + curbH, rcOut0.z, rcOut0.x, 0f, rcOut0.z, rcOut1.x, 0f, rcOut1.z, 0.72f, 0.45f, 0.25f)
            }
        }

        val sn = track.nodes[0]
        val hw = sn.halfWidth + 1.2f
        val leftPillar = sn.center - sn.right * hw
        val rightPillar = sn.center + sn.right * hw
        acc.addCylinder(leftPillar.x, leftPillar.y, leftPillar.z, 0.9f, 0.9f, 10.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
        acc.addCylinder(rightPillar.x, rightPillar.y, rightPillar.z, 0.9f, 0.9f, 10.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
        val bL = leftPillar + Vec3(0f, 9.2f, 0f)
        val bR = rightPillar + Vec3(0f, 9.2f, 0f)
        val segs = 12
        for (s in 0 until segs) {
            val t0 = s.toFloat() / segs
            val t1 = (s + 1).toFloat() / segs
            val p0 = bL * (1f - t0) + bR * t0
            val p1 = bL * (1f - t1) + bR * t1
            val (br, bg, bb) = if (s % 2 == 0) Triple(1f, 0.22f, 0.55f) else Triple(1f, 0.96f, 0.98f)
            acc.addQuad(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p1.x, p1.y + 2.2f, p1.z, p0.x, p0.y + 2.2f, p0.z, br, bg, bb, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE)
        }

        return acc.build()
    }

    fun buildGroundMesh(trackId: com.example.sweetrush.model.TrackId): GpuMesh {
        val acc = MeshAccumulator()
        val spec = CatalogData.trackById(trackId)
        val (gr, gg, gb) = MeshAccumulator.hexToRgb(spec.groundHex)
        val grid = 16
        val minCoord = -460f
        val maxCoord = 640f
        val step = (maxCoord - minCoord) / grid
        for (ix in 0 until grid) {
            for (iz in 0 until grid) {
                val x0 = minCoord + ix * step
                val x1 = x0 + step
                val z0 = minCoord + iz * step
                val z1 = z0 + step
                val alt = if ((ix + iz) % 2 == 0) 1.0f else 0.92f
                acc.addQuad(x0, -0.15f, z1, x1, -0.15f, z1, x1, -0.15f, z0, x0, -0.15f, z0, gr * alt, gg * alt, gb * alt)
            }
        }
        return acc.build()
    }

    fun buildBoostPadMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0.04f, 0f, 3.6f, 0.08f, 4.4f, 0xFFFF3D8B, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addBox(0f, 0.07f, 0.6f, 2.4f, 0.08f, 0.9f, 0xFFFFEB3B, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addBox(0f, 0.07f, -0.8f, 2.4f, 0.08f, 0.9f, 0xFF3CE8B5, matType = CandyMaterials.MAT_EMISSIVE)
        return acc.build()
    }

    fun buildJumpRampMesh(): GpuMesh {
        val acc = MeshAccumulator()
        val r = 1f; val g = 0.58f; val b = 0.12f
        val hw = 2.8f; val hl = 2.8f; val h = 1.25f
        acc.addQuad(-hw, 0f, -hl, hw, 0f, -hl, hw, h, hl, -hw, h, hl, r, g, b, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_CANDY)
        acc.addQuad(-hw, 0f, hl, hw, 0f, hl, hw, h, hl, -hw, h, hl, 1f, 0.25f, 0.55f, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE)
        acc.addTri(-hw, 0f, -hl, -hw, 0f, hl, -hw, h, hl, 1f, 0.9f, 0.3f)
        acc.addTri(hw, 0f, -hl, hw, h, hl, hw, 0f, hl, 1f, 0.9f, 0.3f)
        return acc.build()
    }

    fun buildHazardMesh(isCaramel: Boolean): GpuMesh {
        val acc = MeshAccumulator()
        if (isCaramel) {
            acc.addCylinder(0f, 0.02f, 0f, 2.2f, 2.1f, 0.12f, 12, 0xFFFF9800, 0xFFFFB74D, matType = CandyMaterials.MAT_LIQUID)
            acc.addSphere(0.5f, 0.18f, 0.3f, 0.35f, 0.22f, 0.35f, 5, 6, 0xFFFFCC80, matType = CandyMaterials.MAT_GUMMY)
        } else {
            acc.addCylinder(0f, 0.02f, 0f, 1.55f, 1.55f, 0.32f, 12, 0xFFFF2448, 0xFFFFFFFF)
        }
        return acc.build()
    }

    fun buildItemBoxMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0f, 0f, 1.45f, 1.45f, 1.45f, 0xFFFFEB3B, 0.90f, CandyMaterials.MAT_GUMMY)
        acc.addBox(0f, 0f, 0f, 1.56f, 0.35f, 1.56f, 0xFFFF3D8B, 0.95f, CandyMaterials.MAT_EMISSIVE)
        acc.addBox(0f, 0f, 0f, 0.35f, 1.56f, 1.56f, 0xFF3CE8B5, 0.95f, CandyMaterials.MAT_EMISSIVE)
        return acc.build()
    }

    fun buildSimpleSphereMesh(hex: Long, alpha: Float, matType: Float = CandyMaterials.MAT_GUMMY): GpuMesh {
        val acc = MeshAccumulator()
        acc.addSphere(0f, 0f, 0f, 1f, 1f, 1f, 8, 12, hex, hex, alpha, matType)
        return acc.build()
    }

    fun buildSimpleCubeMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0f, 0f, 1f, 1f, 1f, 0xFFFFFFFF)
        return acc.build()
    }

    fun buildWorldCoinMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addHorizontalCylinderZ(0f, 0f, 0f, 0.72f, 0.72f, 0.22f, 14, 0xFFFFD700, 0xFFFFB300, CandyMaterials.MAT_EMISSIVE)
        acc.addHorizontalCylinderZ(0f, 0f, 0f, 0.44f, 0.44f, 0.26f, 10, 0xFFFFF8E1, 0xFFFFF8E1, CandyMaterials.MAT_CHROME)
        return acc.build()
    }

    fun buildWorldStarMesh(): GpuMesh {
        val acc = MeshAccumulator()
        // 5-pointed faceted candy star crystal
        val outerR = 1.25f
        val innerR = 0.55f
        val depth = 0.32f
        val (r, g, b) = MeshAccumulator.hexToRgb(0xFFFFEA00)
        for (i in 0 until 10) {
            val a0 = (i * 36f) * PI.toFloat() / 180f
            val a1 = ((i + 1) * 36f) * PI.toFloat() / 180f
            val rad0 = if (i % 2 == 0) outerR else innerR
            val rad1 = if ((i + 1) % 2 == 0) outerR else innerR
            val x0 = sin(a0) * rad0; val y0 = cos(a0) * rad0
            val x1 = sin(a1) * rad1; val y1 = cos(a1) * rad1
            acc.addTri(0f, 0f, depth, x0, y0, 0f, x1, y1, 0f, r, g, b, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, CandyMaterials.MAT_EMISSIVE)
            acc.addTri(0f, 0f, -depth, x1, y1, 0f, x0, y0, 0f, r * 0.9f, g * 0.85f, b, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, CandyMaterials.MAT_EMISSIVE)
        }
        return acc.build()
    }

    fun buildRacePortalMesh(accentHex: Long): GpuMesh {
        val acc = MeshAccumulator()
        acc.addCylinder(-4.5f, 0f, 0f, 0.7f, 0.6f, 6.2f, 10, 0xFFFFFFFF, accentHex)
        acc.addCylinder(4.5f, 0f, 0f, 0.7f, 0.6f, 6.2f, 10, 0xFFFFFFFF, accentHex)
        acc.addTorus(0f, 6.2f, 0f, 4.5f, 0.75f, 16, 8, accentHex, 0xFFFFFFFF, verticalArch = true, matType = CandyMaterials.MAT_EMISSIVE)
        // Translucent swirling portal field
        acc.addCylinder(0f, 0.1f, 0f, 3.8f, 3.8f, 0.25f, 18, accentHex, 0xFFFFFFFF, a = 0.65f, matType = CandyMaterials.MAT_EMISSIVE)
        return acc.build()
    }

    fun buildDecorationMesh(type: DecoType, c1: Long, c2: Long): GpuMesh {
        val acc = MeshAccumulator()
        val contactShadowR = when (type) {
            DecoType.CANDY_CANE_ARCH, DecoType.DONUT_ARCH, DecoType.WAFER_TUNNEL_RING, DecoType.FLOATING_BALLOON -> 0f
            DecoType.CANDY_FLOWER -> 0.8f
            DecoType.CANDY_LAMP, DecoType.ROAD_SIGN -> 1.1f
            DecoType.CANDY_BARRIER, DecoType.CANDY_BENCH, DecoType.CANDY_CANE_PLANT -> 2.1f
            DecoType.MARSHMALLOW_BUSH, DecoType.JELLYBEAN_BUSH, DecoType.SUGAR_CRYSTAL -> 2.4f
            DecoType.LOLLIPOP_TREE, DecoType.GUMDROP_TREE, DecoType.COTTON_CANDY_TREE, DecoType.CHOCOLATE_TREE -> 3.0f
            DecoType.GUMMY_BEAR_STATUE, DecoType.PEPPERMINT_LIGHTHOUSE -> 3.8f
            DecoType.GINGERBREAD_HOUSE, DecoType.COOKIE_HOUSE, DecoType.DONUT_BUILDING,
            DecoType.ICE_CREAM_PARLOR, DecoType.MACARON_TOWER, DecoType.CUPCAKE_TOWER -> 5.0f
            DecoType.STRAWBERRY_FOUNTAIN, DecoType.CHOCOLATE_WATERFALL, DecoType.WAFFLE_SKYSCRAPER,
            DecoType.SPECTATOR_STAND, DecoType.ANIMATED_BILLBOARD -> 5.4f
            DecoType.CHOCOLATE_CASTLE, DecoType.GUMDROP_HILL, DecoType.CANDY_FACTORY -> 6.8f
        }
        if (contactShadowR > 0f) {
            acc.addShadowOval(contactShadowR, contactShadowR, segs = 14, alpha = 0.34f)
        }
        when (type) {
            DecoType.LOLLIPOP_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.35f, 0.30f, 6.2f, 8, 0xFFFFFFFF, matType = CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, 7.6f, 0f, 2.4f, 2.4f, 0.85f, 8, 12, c1, 0xFFFFFFFF)
                acc.addTorus(0f, 7.6f, 0f, 2.35f, 0.32f, 14, 6, c2, c2, verticalArch = true)
            }
            DecoType.GUMDROP_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.45f, 0.38f, 3.2f, 8, 0xFF6D4C41)
                acc.addCylinder(0f, 3.0f, 0f, 2.2f, 0.4f, 3.8f, 10, c1, c2, matType = CandyMaterials.MAT_GUMMY)
            }
            DecoType.GUMDROP_HILL -> {
                acc.addCylinder(0f, 0f, 0f, 6.5f, 2.2f, 7.2f, 12, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(0f, 7.0f, 0f, 2.3f, 1.8f, 2.3f, 6, 10, c1, c2, matType = CandyMaterials.MAT_GUMMY)
            }
            DecoType.MARSHMALLOW_BUSH -> {
                acc.addCylinder(-0.9f, 0f, 0f, 1.1f, 1.1f, 1.6f, 10, 0xFFFFF8FF, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0.9f, 0f, 0.4f, 1.0f, 1.0f, 1.9f, 10, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, 0f, -0.7f, 1.1f, 1.1f, 2.2f, 10, 0xFFFFF8FF, matType = CandyMaterials.MAT_MARSHMALLOW)
            }
            DecoType.COTTON_CANDY_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.32f, 0.28f, 4.5f, 8, 0xFFFFE0B2, matType = CandyMaterials.MAT_WAFER)
                acc.addSphere(0f, 5.8f, 0f, 2.6f, 2.2f, 2.6f, 7, 10, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(1.4f, 5.1f, 0.9f, 1.6f, 1.5f, 1.6f, 6, 8, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(-1.3f, 5.2f, -0.8f, 1.5f, 1.4f, 1.5f, 6, 8, 0xFFFFB8DE, matType = CandyMaterials.MAT_MARSHMALLOW)
            }
            DecoType.CANDY_CANE_PLANT -> {
                acc.addCylinder(0f, 0f, 0f, 0.34f, 0.34f, 4.4f, 8, 0xFFFF2448, 0xFFFFFFFF)
                acc.addTorus(0.9f, 4.4f, 0f, 0.9f, 0.34f, 10, 6, 0xFFFF2448, 0xFFFFFFFF, verticalArch = true)
            }
            DecoType.CHOCOLATE_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.55f, 0.35f, 4.8f, 8, 0xFF4E2A1E, matType = CandyMaterials.MAT_WAFER)
                acc.addSphere(0f, 5.6f, 0f, 2.5f, 1.8f, 2.5f, 7, 10, 0xFF6D3B22, c1, matType = CandyMaterials.MAT_CARAMEL)
            }
            DecoType.JELLYBEAN_BUSH -> {
                acc.addCylinder(0f, 0f, 0f, 1.65f, 1.4f, 0.45f, 10, 0xFF2ED8B6, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(-0.65f, 1.15f, 0.35f, 0.72f, 0.95f, 0.68f, 6, 8, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(0.68f, 1.25f, 0.15f, 0.70f, 1.05f, 0.65f, 6, 8, c2, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(0.05f, 1.50f, -0.45f, 0.76f, 1.12f, 0.72f, 6, 8, 0xFFFFEB3B, matType = CandyMaterials.MAT_GUMMY)
            }
            DecoType.CANDY_FLOWER -> {
                acc.addCylinder(0f, 0f, 0f, 0.12f, 0.10f, 1.8f, 6, 0xFF3CE8B5, matType = CandyMaterials.MAT_CANDY)
                for (p in 0 until 5) {
                    val ang = p * (2f * PI.toFloat() / 5f)
                    val px = cos(ang) * 0.58f
                    val pz = sin(ang) * 0.58f
                    acc.addSphere(px, 1.85f, pz, 0.34f, 0.16f, 0.34f, 4, 6, if (p % 2 == 0) c1 else c2, matType = CandyMaterials.MAT_CANDY)
                }
                acc.addSphere(0f, 1.92f, 0f, 0.28f, 0.22f, 0.28f, 5, 8, 0xFFFFEA00, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.GINGERBREAD_HOUSE -> {
                // Stone/wafer foundation + two-story spiced gingerbread walls + frosted eaves + glowing candy windows
                acc.addBox(0f, 0.35f, 0f, 6.8f, 0.7f, 6.2f, 0xFF6D3B22, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBox(0f, 2.8f, 0f, 6.2f, 4.4f, 5.6f, 0xFFB86F3A, matType = CandyMaterials.MAT_WAFER)
                acc.addCylinder(0f, 5.0f, 0f, 5.2f, 0.2f, 3.6f, 4, 0xFFFFF8FF, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBox(0f, 5.0f, 0f, 6.7f, 0.42f, 6.1f, 0xFFFF9800, matType = CandyMaterials.MAT_CARAMEL)
                // Chimney with marshmallow puff
                acc.addBox(2.0f, 6.8f, -1.2f, 0.95f, 2.2f, 0.95f, 0xFF8B4726, matType = CandyMaterials.MAT_WAFER)
                acc.addSphere(2.0f, 8.3f, -1.2f, 0.75f, 0.55f, 0.75f, 5, 8, 0xFFFFF8FF, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Door + frosted awning + multi-facade windows
                acc.addBox(0f, 1.6f, 2.86f, 1.6f, 2.6f, 0.25f, c2)
                acc.addBox(0f, 3.05f, 3.15f, 2.6f, 0.35f, 1.0f, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBox(-1.9f, 2.9f, 2.86f, 1.15f, 1.2f, 0.2f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(1.9f, 2.9f, 2.86f, 1.15f, 1.2f, 0.2f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(-3.14f, 2.9f, 0f, 0.16f, 1.2f, 1.8f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(3.14f, 2.9f, 0f, 0.16f, 1.2f, 1.8f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.CUPCAKE_TOWER -> {
                acc.addCylinder(0f, 0f, 0f, 3.8f, 4.4f, 5.8f, 14, c1, 0xFFFFE0EC, matType = CandyMaterials.MAT_WAFER)
                acc.addCylinder(0f, 5.6f, 0f, 4.6f, 4.2f, 1.6f, 14, 0xFFFFF8FF, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 7.6f, 0f, 4.1f, 2.6f, 4.1f, 7, 12, 0xFFFFF8FF, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 10.6f, 0f, 1.05f, 1.05f, 1.05f, 6, 8, 0xFFFF1744, matType = CandyMaterials.MAT_CARAMEL)
                // Illuminated arched shopfront
                acc.addBox(0f, 1.6f, 3.8f, 2.2f, 2.8f, 0.4f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.COOKIE_HOUSE -> {
                acc.addBox(0f, 2.1f, 0f, 6.2f, 4.2f, 6.2f, 0xFFD99B52, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 4.4f, 0f, 6.8f, 0.95f, 6.8f, 0xFF4E2A1E, matType = CandyMaterials.MAT_CARAMEL)
                acc.addSphere(-1.8f, 4.9f, 1.6f, 0.55f, 0.35f, 0.55f, 4, 6, 0xFF3E1C10)
                acc.addSphere(1.5f, 4.9f, -1.4f, 0.55f, 0.35f, 0.55f, 4, 6, 0xFF3E1C10)
                acc.addSphere(0f, 2.1f, 3.15f, 1.0f, 1.0f, 0.3f, 5, 8, c1)
                acc.addBox(-1.9f, 2.5f, 3.15f, 1.0f, 1.0f, 0.16f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(1.9f, 2.5f, 3.15f, 1.0f, 1.0f, 0.16f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.CHOCOLATE_CASTLE -> {
                acc.addBox(0f, 4.2f, 0f, 8.8f, 8.4f, 8.8f, 0xFF4A2518, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 2.4f, 4.46f, 3.0f, 4.2f, 0.25f, 0xFFFFB300, matType = CandyMaterials.MAT_CARAMEL)
                for (sx in listOf(-4.1f, 4.1f)) {
                    for (sz in listOf(-4.1f, 4.1f)) {
                        acc.addCylinder(sx, 0f, sz, 1.45f, 1.3f, 11.2f, 10, 0xFF6B3A26, matType = CandyMaterials.MAT_CARAMEL)
                        acc.addCylinder(sx, 11.2f, sz, 1.75f, 0.1f, 2.8f, 10, c1)
                    }
                }
            }
            DecoType.DONUT_BUILDING -> {
                acc.addCylinder(0f, 0f, 0f, 3.9f, 3.9f, 2.8f, 12, 0xFFF4B678, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 1.4f, 3.7f, 2.4f, 2.4f, 0.35f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addTorus(0f, 5.2f, 0f, 3.0f, 1.35f, 16, 8, 0xFFF4B678, c1, verticalArch = true, matType = CandyMaterials.MAT_CARAMEL)
            }
            DecoType.CANDY_FACTORY -> {
                acc.addBox(0f, 3.4f, 0f, 11.0f, 6.8f, 8.0f, 0xFFB86F3A, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 7.0f, 0f, 11.8f, 1.0f, 8.6f, 0xFFFFF8FF, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(-3.4f, 7.3f, -1.6f, 1.0f, 0.78f, 5.6f, 10, 0xFFFF2448, 0xFFFFFFFF)
                acc.addCylinder(3.4f, 7.3f, -1.6f, 1.0f, 0.78f, 5.6f, 10, c1, 0xFFFFFFFF)
                acc.addSphere(-3.4f, 13.7f, -1.6f, 1.45f, 1.05f, 1.45f, 5, 8, 0xFFFFE0EC, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(3.4f, 14.1f, -1.6f, 1.45f, 1.05f, 1.45f, 5, 8, 0xFFE0FFF5, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBox(0f, 3.4f, 4.08f, 7.6f, 2.3f, 0.2f, c2, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(0f, 1.3f, 4.06f, 2.6f, 2.6f, 0.25f, 0xFF4E2A1E, matType = CandyMaterials.MAT_CARAMEL)
            }
            DecoType.GUMMY_BEAR_STATUE -> {
                acc.addSphere(0f, 2.6f, 0f, 2.1f, 2.6f, 1.7f, 7, 10, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(0f, 5.6f, 0f, 1.7f, 1.6f, 1.5f, 7, 10, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(-1.3f, 7.0f, 0f, 0.65f, 0.65f, 0.45f, 5, 8, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(1.3f, 7.0f, 0f, 0.65f, 0.65f, 0.45f, 5, 8, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(0f, 5.3f, 1.35f, 0.75f, 0.55f, 0.55f, 5, 8, c2, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(-1.9f, 3.2f, 0.6f, 0.7f, 1.1f, 0.7f, 5, 8, c1, matType = CandyMaterials.MAT_GUMMY)
                acc.addSphere(1.9f, 3.2f, 0.6f, 0.7f, 1.1f, 0.7f, 5, 8, c1, matType = CandyMaterials.MAT_GUMMY)
            }
            DecoType.SUGAR_CRYSTAL -> {
                acc.addCylinder(0f, 0f, 0f, 1.2f, 0.08f, 5.8f, 6, c1, 0xFFFFFFFF, matType = CandyMaterials.MAT_GUMMY)
                acc.addCylinder(1.2f, 0f, 0.6f, 0.8f, 0.05f, 3.9f, 6, c2, 0xFFFFFFFF, matType = CandyMaterials.MAT_GUMMY)
                acc.addCylinder(-1.1f, 0f, -0.5f, 0.9f, 0.05f, 4.2f, 6, c1, matType = CandyMaterials.MAT_GUMMY)
            }
            DecoType.FLOATING_BALLOON -> {
                acc.addSphere(0f, 0f, 0f, 2.2f, 2.6f, 2.2f, 8, 12, c1, c2)
                acc.addCylinder(0f, -3.5f, 0f, 0.06f, 0.06f, 1.2f, 5, 0xFFFFFFFF)
            }
            DecoType.CHOCOLATE_WATERFALL -> {
                acc.addBox(0f, 6.0f, -2.0f, 7.0f, 12.0f, 4.0f, 0xFF4A2315)
                acc.addBox(0f, 6.0f, 0.3f, 4.4f, 11.8f, 0.8f, 0xFF8B4726, matType = CandyMaterials.MAT_LIQUID)
                acc.addCylinder(0f, 0f, 1.8f, 5.0f, 5.0f, 0.5f, 12, 0xFF9C522B, 0xFFFFB74D, matType = CandyMaterials.MAT_LIQUID)
            }
            DecoType.STRAWBERRY_FOUNTAIN -> {
                acc.addCylinder(0f, 0f, 0f, 4.6f, 4.2f, 1.2f, 14, 0xFFFFF8FF)
                acc.addCylinder(0f, 1.0f, 0f, 1.1f, 0.9f, 3.6f, 10, 0xFFFF6EB4, matType = CandyMaterials.MAT_LIQUID)
                acc.addCylinder(0f, 3.8f, 0f, 2.4f, 2.6f, 0.8f, 12, 0xFFFF3D8B, matType = CandyMaterials.MAT_LIQUID)
            }
            DecoType.CANDY_LAMP -> {
                acc.addCylinder(0f, 0f, 0f, 0.22f, 0.16f, 4.8f, 8, 0xFFFFFFFF, 0xFFFF3D8B, matType = CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, 5.2f, 0f, 0.72f, 0.72f, 0.72f, 6, 8, c1, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.CANDY_CANE_ARCH -> {
                acc.addCylinder(-14.2f, 0f, 0f, 0.9f, 0.9f, 7.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
                acc.addCylinder(14.2f, 0f, 0f, 0.9f, 0.9f, 7.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
                val segs = 14
                for (s in 0 until segs) {
                    val a0 = (s.toFloat() / segs) * PI.toFloat()
                    val a1 = ((s + 1).toFloat() / segs) * PI.toFloat()
                    val x0 = -cos(a0) * 14.2f; val y0 = 7.5f + sin(a0) * 8.5f
                    val x1 = -cos(a1) * 14.2f; val y1 = 7.5f + sin(a1) * 8.5f
                    val col = if (s % 2 == 0) 0xFFFF2448L else 0xFFFFFFFFL
                    val (r, g, b) = MeshAccumulator.hexToRgb(col)
                    acc.addQuad(x0, y0, -0.7f, x1, y1, -0.7f, x1, y1 + 1.4f, 0.7f, x0, y0 + 1.4f, 0.7f, r, g, b)
                }
            }
            DecoType.DONUT_ARCH -> {
                acc.addTorus(0f, 4.5f, 0f, 14.5f, 1.55f, 18, 8, 0xFFF4B678, c1, verticalArch = true, matType = CandyMaterials.MAT_CARAMEL)
            }
            DecoType.WAFER_TUNNEL_RING -> {
                acc.addBox(-14.2f, 5.0f, 0f, 1.4f, 10.0f, 7.0f, c1, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(14.2f, 5.0f, 0f, 1.4f, 10.0f, 7.0f, c1, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 10.4f, 0f, 29.8f, 1.5f, 7.0f, c2, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 9.5f, 0f, 24.0f, 0.28f, 0.45f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.SPECTATOR_STAND -> {
                acc.addBox(0f, 0.7f, 0f, 10f, 1.4f, 4.6f, 0xFF6A1B9A)
                acc.addBox(0f, 1.9f, -0.9f, 10f, 1.2f, 2.8f, 0xFF8E24AA)
                acc.addBox(0f, 3.0f, -1.7f, 10f, 1.0f, 1.4f, 0xFFAB47BC)
                val crowdColors = listOf(c1, c2, 0xFFFFEB3BL, 0xFF3CE8B5L, 0xFFFF3D8BL)
                for (row in 0..1) {
                    val sy = if (row == 0) 1.7f else 2.8f
                    val sz = if (row == 0) 0.8f else -0.8f
                    for (col in -3..3 step 2) {
                        val colHex = crowdColors[(col + row + 10) % crowdColors.size]
                        acc.addSphere(col * 1.15f, sy, sz, 0.42f, 0.52f, 0.38f, 5, 6, colHex)
                        acc.addSphere(col * 1.15f, sy + 0.65f, sz, 0.30f, 0.30f, 0.30f, 5, 6, 0xFFFFD1B3)
                    }
                }
            }
            DecoType.ANIMATED_BILLBOARD -> {
                acc.addCylinder(-2.6f, 0f, 0f, 0.35f, 0.3f, 7.2f, 8, 0xFFCFD8DC, matType = CandyMaterials.MAT_CHROME)
                acc.addCylinder(2.6f, 0f, 0f, 0.35f, 0.3f, 7.2f, 8, 0xFFCFD8DC, matType = CandyMaterials.MAT_CHROME)
                acc.addBox(0f, 7.5f, 0f, 7.4f, 3.8f, 0.55f, 0xFF28113B)
                acc.addBox(0f, 7.5f, 0.30f, 6.6f, 3.0f, 0.10f, c1, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(0f, 7.5f, -0.30f, 6.6f, 3.0f, 0.10f, c2, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.CANDY_BENCH -> {
                acc.addBox(0f, 0.55f, 0f, 3.2f, 0.24f, 1.0f, c1, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 1.15f, -0.45f, 3.2f, 0.9f, 0.22f, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBox(-1.35f, 0.25f, 0f, 0.24f, 0.5f, 0.9f, 0xFFFFFFFF, matType = CandyMaterials.MAT_CHROME)
                acc.addBox(1.35f, 0.25f, 0f, 0.24f, 0.5f, 0.9f, 0xFFFFFFFF, matType = CandyMaterials.MAT_CHROME)
            }
            DecoType.ROAD_SIGN -> {
                acc.addCylinder(0f, 0f, 0f, 0.14f, 0.12f, 3.8f, 6, 0xFFCFD8DC, matType = CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 4.1f, 0f, 2.3f, 1.35f, 0.22f, 0.95f, 0.95f, c1, c1)
                acc.addBox(0f, 4.1f, 0.13f, 1.85f, 0.92f, 0.06f, 0xFFFFF8FF, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(0.32f, 4.1f, 0.17f, 0.65f, 0.28f, 0.04f, c2, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.CANDY_BARRIER -> {
                acc.addCylinder(-1.7f, 0f, 0f, 0.16f, 0.14f, 1.15f, 6, 0xFFCFD8DC, matType = CandyMaterials.MAT_CHROME)
                acc.addCylinder(1.7f, 0f, 0f, 0.16f, 0.14f, 1.15f, 6, 0xFFCFD8DC, matType = CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(-0.9f, 0.65f, 0f, 1.8f, 0.68f, 0.34f, 0.92f, 0.92f, c1)
                acc.addBeveledBox(0.9f, 0.65f, 0f, 1.8f, 0.68f, 0.34f, 0.92f, 0.92f, 0xFFFFFFFF)
                acc.addBox(0f, 0.72f, 0.18f, 3.2f, 0.14f, 0.05f, c2, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.ICE_CREAM_PARLOR -> {
                // Waffle cone café base + striped canopy + giant twin ice cream scoop roof + neon parlor windows
                acc.addBox(0f, 2.3f, 0f, 7.2f, 4.6f, 6.2f, 0xFFFFF0F6, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBox(0f, 4.7f, 0f, 7.8f, 0.7f, 6.8f, c1, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBox(0f, 3.1f, 3.45f, 6.4f, 0.55f, 1.2f, c2)
                acc.addBox(0f, 1.5f, 3.15f, 2.2f, 2.6f, 0.2f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(-2.2f, 2.4f, 3.15f, 1.4f, 1.5f, 0.2f, 0xFF7BE8FF, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(2.2f, 2.4f, 3.15f, 1.4f, 1.5f, 0.2f, 0xFF7BE8FF, matType = CandyMaterials.MAT_EMISSIVE)
                // Giant upside-down waffle cone & double scoop dome
                acc.addSphere(0f, 6.6f, 0f, 2.7f, 2.2f, 2.7f, 7, 12, c1, 0xFFFFF8FF, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 9.0f, 0f, 1.9f, 1.6f, 1.9f, 6, 10, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, 10.2f, 0f, 1.25f, 0.12f, 3.2f, 10, 0xFFD99B52, matType = CandyMaterials.MAT_WAFER)
            }
            DecoType.MACARON_TOWER -> {
                // Multi-tiered pastel macaron boutique building with creamy filling rings & illuminated balcony
                acc.addCylinder(0f, 0f, 0f, 4.4f, 4.4f, 2.0f, 16, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, 2.0f, 0f, 4.0f, 4.0f, 0.9f, 16, 0xFFFFF8FF, matType = CandyMaterials.MAT_CARAMEL)
                acc.addCylinder(0f, 2.9f, 0f, 3.8f, 3.8f, 2.2f, 16, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, 5.1f, 0f, 3.4f, 3.4f, 0.85f, 16, 0xFFFFF8FF, matType = CandyMaterials.MAT_CARAMEL)
                acc.addCylinder(0f, 5.95f, 0f, 3.1f, 3.1f, 2.0f, 16, c1, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 8.2f, 0f, 2.4f, 1.4f, 2.4f, 6, 12, 0xFFFFEB3B, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBox(0f, 1.3f, 4.28f, 2.0f, 2.2f, 0.35f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addBox(0f, 4.0f, 3.72f, 2.4f, 1.0f, 0.25f, 0xFF80FFFF, matType = CandyMaterials.MAT_EMISSIVE)
            }
            DecoType.PEPPERMINT_LIGHTHOUSE -> {
                // Tall candy-cane striped coastal & plaza landmark tower with glowing beacon lantern
                acc.addCylinder(0f, 0f, 0f, 3.2f, 2.5f, 5.0f, 12, 0xFFFF2448, 0xFFFFFFFF)
                acc.addCylinder(0f, 5.0f, 0f, 2.5f, 1.9f, 5.0f, 12, 0xFFFFFFFF, 0xFFFF2448)
                acc.addCylinder(0f, 10.0f, 0f, 2.4f, 2.6f, 0.7f, 12, c1, matType = CandyMaterials.MAT_CHROME)
                acc.addCylinder(0f, 10.7f, 0f, 1.55f, 1.55f, 2.1f, 12, 0xFFFFEA00, matType = CandyMaterials.MAT_EMISSIVE)
                acc.addCylinder(0f, 12.8f, 0f, 2.0f, 0.1f, 2.4f, 12, c2, matType = CandyMaterials.MAT_CARAMEL)
            }
            DecoType.WAFFLE_SKYSCRAPER -> {
                // Towering golden waffle high-rise with caramel trim, skybridge ribs & glowing neon windows
                acc.addBox(0f, 6.5f, 0f, 7.6f, 13.0f, 7.6f, 0xFFD99B52, matType = CandyMaterials.MAT_WAFER)
                acc.addBox(0f, 13.2f, 0f, 8.2f, 0.9f, 8.2f, c1, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBox(0f, 15.6f, 0f, 5.2f, 4.2f, 5.2f, 0xFFF4B678, matType = CandyMaterials.MAT_WAFER)
                acc.addCylinder(0f, 17.7f, 0f, 0.45f, 0.12f, 3.8f, 8, c2, matType = CandyMaterials.MAT_EMISSIVE)
                // Illuminated window bands on all 4 facades
                for (floorY in listOf(3.0f, 6.5f, 10.0f)) {
                    acc.addBox(0f, floorY, 3.86f, 5.8f, 1.35f, 0.14f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addBox(0f, floorY, -3.86f, 5.8f, 1.35f, 0.14f, 0xFFFFF59D, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addBox(3.86f, floorY, 0f, 0.14f, 1.35f, 5.8f, c2, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addBox(-3.86f, floorY, 0f, 0.14f, 1.35f, 5.8f, c2, matType = CandyMaterials.MAT_EMISSIVE)
                }
                acc.addBox(0f, 1.4f, 3.90f, 2.8f, 2.6f, 0.22f, c1, matType = CandyMaterials.MAT_EMISSIVE)
            }
        }
        return acc.build()
    }

    /**
     * Multi-tiered frosted cake & wafer turntable platform for the 3D Kart Bakery Garage.
     */
    fun buildBakeryTurntablePedestalMesh(): GpuMesh {
        val acc = MeshAccumulator()
        // Base chocolate & golden wafer cake tiers
        acc.addCylinder(0f, -0.12f, 0f, 3.15f, 2.95f, 0.16f, 28, 0xFF3A1D28L, 0xFF5A2D3CL, matType = CandyMaterials.MAT_CARAMEL)
        acc.addCylinder(0f, 0.02f, 0f, 2.82f, 2.62f, 0.26f, 28, 0xFFE6A95CL, 0xFFFF85C2L, matType = CandyMaterials.MAT_WAFER)
        // Glossy strawberry frosting top deck + glowing neon trim ring
        acc.addCylinder(0f, 0.26f, 0f, 2.52f, 2.44f, 0.10f, 28, 0xFFFF6EB4L, 0xFFFFF5FAL, matType = CandyMaterials.MAT_CANDY)
        acc.addCylinder(0f, 0.35f, 0f, 2.30f, 2.30f, 0.06f, 28, 0xFF3CE8B5L, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addTorus(0f, 0.32f, 0f, 2.48f, 0.08f, 24, 8, 0xFFFFD54FL, 0xFFFFF8E1L, matType = CandyMaterials.MAT_CHROME)

        // 20 Piped whipped-cream dollops around the turntable perimeter
        val dollops = 20
        for (i in 0 until dollops) {
            val a = (i.toFloat() / dollops) * (2f * PI.toFloat())
            val dx = cos(a) * 2.68f
            val dz = sin(a) * 2.68f
            val col = if (i % 2 == 0) 0xFFFFF8F2L else 0xFFFF9EC7L
            acc.addSphere(dx, 0.28f, dz, 0.16f, 0.14f, 0.16f, 5, 7, col, 0xFFFFFFFFL, matType = CandyMaterials.MAT_MARSHMALLOW)
        }
        return acc.build()
    }

    /**
     * Full 3D Sugar Rush Kart Bakery Workshop environment surrounding the showroom turntable:
     * - Pastel wafer & frosting checkerboard bakery floor
     * - Candy-cane columns & glowing bakery arches
     * - Overhead Frosting & Caramel Piping Nozzles (Kart Bakery dispensers)
     * - Giant stacked Cookie & Frosted Donut ingredient racks + Glowing Gummy Jar Silos
     */
    fun buildGarageBakeryWorkshopMesh(): GpuMesh {
        val acc = MeshAccumulator()
        // 1. Checkerboard Bakery Floor (Wafer Gold & Strawberry Cream tiles)
        val grid = 10
        val minC = -18f
        val maxC = 18f
        val step = (maxC - minC) / grid
        for (ix in 0 until grid) {
            for (iz in 0 until grid) {
                val x0 = minC + ix * step
                val x1 = x0 + step
                val z0 = minC + iz * step
                val z1 = z0 + step
                val isEven = (ix + iz) % 2 == 0
                val hex = if (isEven) 0xFF38184CL else 0xFF4B2166L
                val (r, g, b) = MeshAccumulator.hexToRgb(hex)
                acc.addQuad(
                    x0, -0.38f, z1,
                    x1, -0.38f, z1,
                    x1, -0.38f, z0,
                    x0, -0.38f, z0,
                    r, g, b, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD
                )
            }
        }
        // Decorative glowing outer floor ring around the turntable
        acc.addTorus(0f, -0.34f, 0f, 4.2f, 0.09f, 28, 6, 0xFFFF3D8BL, 0xFF5CE1E6L, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addTorus(0f, -0.34f, 0f, 6.8f, 0.08f, 28, 6, 0xFFFFD54FL, 0xFFFF85C2L, matType = CandyMaterials.MAT_EMISSIVE)

        // 2. Ring of 8 Candy-Cane Bakery Columns & Macaron Capitals
        val colCount = 8
        val colRadius = 9.5f
        for (i in 0 until colCount) {
            val a = (i.toFloat() / colCount) * (2f * PI.toFloat()) + 0.39f
            val cx = cos(a) * colRadius
            val cz = sin(a) * colRadius
            val accent = if (i % 2 == 0) 0xFFFF2448L else 0xFF3CE8B5L
            // Wafer pedestal base
            acc.addCylinder(cx, -0.38f, cz, 0.78f, 0.68f, 0.75f, 10, 0xFFE6A95CL, 0xFFFFD54FL, matType = CandyMaterials.MAT_WAFER)
            // Striped candy column shaft
            acc.addCylinder(cx, 0.35f, cz, 0.46f, 0.44f, 5.2f, 10, accent, 0xFFFFFFFFL, matType = CandyMaterials.MAT_CANDY)
            // Macaron capital & glowing globe lamp
            acc.addCylinder(cx, 5.55f, cz, 0.75f, 0.75f, 0.45f, 12, 0xFFFF85C2L, 0xFFFFF8F2L, matType = CandyMaterials.MAT_MARSHMALLOW)
            acc.addSphere(cx * 0.86f, 4.6f, cz * 0.86f, 0.36f, 0.36f, 0.36f, 6, 8, 0xFFFFF59DL, matType = CandyMaterials.MAT_EMISSIVE)
        }

        // 3. Overhead Sugar Rush Kart Bakery Frosting & Sprinkle Piping Nozzles
        val nozzleAngles = listOf(-0.65f, 0f, 0.65f)
        val nozzleColors = listOf(0xFFFF3D8BL, 0xFF3CE8B5L, 0xFFFFD54FL)
        for ((idx, ang) in nozzleAngles.withIndex()) {
            val nx = sin(ang) * 5.6f
            val nz = -cos(ang) * 5.6f
            val col = nozzleColors[idx]
            // Suspended frosting hopper cone & chrome piping tip
            acc.addCylinder(nx, 5.2f, nz, 0.95f, 1.35f, 2.0f, 12, col, 0xFFFFF8F2L, matType = CandyMaterials.MAT_MARSHMALLOW)
            acc.addCylinder(nx, 4.3f, nz, 0.25f, 0.85f, 0.9f, 10, 0xFFE5EAF0L, 0xFFFFFFFFL, matType = CandyMaterials.MAT_CHROME)
            // Dripping frosting dollop at nozzle tip
            acc.addSphere(nx, 4.05f, nz, 0.28f, 0.36f, 0.28f, 6, 8, col, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
        }

        // 4. Translucent Candy-Jar Ingredient Silos & Giant Donut/Wheel Racks around the Workshop
        for (side in listOf(-1f, 1f)) {
            // Candy Jar Silo filled with giant glowing gumballs
            val jx = side * 6.4f
            val jz = -3.8f
            acc.addCylinder(jx, -0.38f, jz, 1.15f, 1.15f, 0.45f, 12, 0xFFE5EAF0L, matType = CandyMaterials.MAT_CHROME)
            acc.addCylinder(jx, 0.07f, jz, 1.05f, 1.05f, 3.2f, 12, 0xFF80FFEAL, 0xFFFFFFFFL, a = 0.55f, matType = CandyMaterials.MAT_GUMMY)
            acc.addCylinder(jx, 3.27f, jz, 1.18f, 0.85f, 0.45f, 12, 0xFFFF3D8BL, matType = CandyMaterials.MAT_CANDY)
            val gumballCols = listOf(0xFFFF3D8BL, 0xFFFFD54FL, 0xFF3CE8B5L, 0xFFB388FFL)
            for (g in 0 until 4) {
                acc.addSphere(
                    jx + (if (g % 2 == 0) -0.25f else 0.25f),
                    0.55f + g * 0.65f,
                    jz + (if (g < 2) 0.2f else -0.2f),
                    0.38f, 0.38f, 0.38f, 5, 8,
                    gumballCols[g],
                    matType = CandyMaterials.MAT_EMISSIVE
                )
            }

            // Giant Stacked Frosted Donut & Cookie Wheel Display Rack
            val rx = side * 7.2f
            val rz = 2.2f
            acc.addCylinder(rx, -0.38f, rz, 0.22f, 0.22f, 3.6f, 8, 0xFFE5EAF0L, matType = CandyMaterials.MAT_CHROME)
            acc.addTorus(rx, 0.35f, rz, 0.85f, 0.32f, 14, 8, 0xFFE6A95CL, 0xFFFF3D8BL, matType = CandyMaterials.MAT_CARAMEL)
            acc.addTorus(rx, 1.10f, rz, 0.80f, 0.30f, 14, 8, 0xFFE6A95CL, 0xFF3CE8B5L, matType = CandyMaterials.MAT_CARAMEL)
            acc.addTorus(rx, 1.85f, rz, 0.75f, 0.28f, 14, 8, 0xFFE6A95CL, 0xFFFFD54FL, matType = CandyMaterials.MAT_CARAMEL)
        }

        // 5. Rear Illuminated "KART BAKERY" Archway Backdrop
        acc.addTorus(0f, 2.4f, -7.8f, 4.6f, 0.42f, 18, 8, 0xFFFF3D8BL, 0xFFFFD54FL, verticalArch = true, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addBeveledBox(0f, 5.2f, -7.5f, 5.4f, 1.25f, 0.45f, 0.95f, 0.95f, 0xFF2B1040L, 0xFFFF3D8BL, CandyMaterials.MAT_CANDY)
        acc.addBox(0f, 5.2f, -7.24f, 4.8f, 0.82f, 0.08f, 0xFFFFE066L, matType = CandyMaterials.MAT_EMISSIVE)

        return acc.build()
    }
}

