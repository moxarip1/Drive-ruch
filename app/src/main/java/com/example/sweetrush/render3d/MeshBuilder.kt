package com.example.sweetrush.render3d

import android.opengl.GLES30
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.KartBodyStyle
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.KartSpec
import com.example.sweetrush.model.RacerSpec
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.track.DecoType
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GpuMesh(
    vertices: FloatArray, // interleaved: x, y, z, nx, ny, nz, r, g, b, a (10 floats per vertex)
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

        val stride = 10 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
    }
}

class MeshAccumulator {
    private val data = ArrayList<Float>(4096)

    fun addTri(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
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
        pushV(x1, y1, z1, nx, ny, nz, r, g, b, a)
        pushV(x2, y2, z2, nx, ny, nz, r, g, b, a)
        pushV(x3, y3, z3, nx, ny, nz, r, g, b, a)
    }

    private fun pushV(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTri(x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a)
        addTri(x1, y1, z1, x3, y3, z3, x4, y4, z4, r, g, b, a)
    }

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, hex: Long, a: Float = 1f) {
        val (r, g, b) = hexToRgb(hex)
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz
        // Front (+Z), Back (-Z), Left (-X), Right (+X), Top (+Y), Bottom (-Y)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a)
    }

    fun addCylinder(
        cx: Float, cy: Float, cz: Float,
        radBot: Float, radTop: Float, height: Float,
        segs: Int, hex: Long, altHex: Long = hex, a: Float = 1f
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

            addQuad(bx0, y0, bz0, bx1, y0, bz1, tx1, y1, tz1, tx0, y1, tz0, r, g, b, a)
            if (radTop > 0.001f) {
                addTri(cx, y1, cz, tx0, y1, tz0, tx1, y1, tz1, r, g, b, a)
            }
            if (radBot > 0.001f) {
                addTri(cx, y0, cz, bx1, y0, bz1, bx0, y0, bz0, r, g, b, a)
            }
        }
    }

    fun addHorizontalWheelCylinder(
        cx: Float, cy: Float, cz: Float,
        radius: Float, width: Float, segs: Int,
        tireHex: Long, rimHex: Long, altRimHex: Long = rimHex
    ) {
        val (tr, tg, tb) = hexToRgb(tireHex)
        val (rr1, rg1, rb1) = hexToRgb(rimHex)
        val (rr2, rg2, rb2) = hexToRgb(altRimHex)
        val x0 = cx - width * 0.5f
        val x1 = cx + width * 0.5f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val y0 = cy + cos(a0) * radius; val z0 = cz + sin(a0) * radius
            val y1 = cy + cos(a1) * radius; val z1 = cz + sin(a1) * radius
            addQuad(x0, y0, z0, x0, y1, z1, x1, y1, z1, x1, y0, z0, tr, tg, tb)

            val (rr, rg, rb) = if (i % 2 == 0) Triple(rr1, rg1, rb1) else Triple(rr2, rg2, rb2)
            val iy0 = cy + cos(a0) * radius * 0.65f; val iz0 = cz + sin(a0) * radius * 0.65f
            val iy1 = cy + cos(a1) * radius * 0.65f; val iz1 = cz + sin(a1) * radius * 0.65f
            addTri(x1 + 0.01f, cy, cz, x1 + 0.01f, iy0, iz0, x1 + 0.01f, iy1, iz1, rr, rg, rb)
            addTri(x0 - 0.01f, cy, cz, x0 - 0.01f, iy1, iz1, x0 - 0.01f, iy0, iz0, rr, rg, rb)
        }
    }

    fun addSphere(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        stacks: Int, slices: Int,
        hex: Long, altHex: Long = hex, a: Float = 1f
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

                addQuad(p00.x, p00.y, p00.z, p10.x, p10.y, p10.z, p11.x, p11.y, p11.z, p01.x, p01.y, p01.z, r, g, b, a)
            }
        }
    }

    fun addTorus(
        cx: Float, cy: Float, cz: Float,
        majorR: Float, minorR: Float,
        majorSegs: Int, minorSegs: Int,
        hex: Long, topGlazeHex: Long = hex,
        verticalArch: Boolean = false
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
                addQuad(p00.x, p00.y, p00.z, p10.x, p10.y, p10.z, p11.x, p11.y, p11.z, p01.x, p01.y, p01.z, r, g, b)
            }
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
        return GpuMesh(arr, data.size / 10)
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

            // Road surface (alternating subtle stripe or finish checkered strip at i == 0)
            if (i == 0 || i == 1) {
                val (fr, fg, fb) = if (i == 0) Triple(1f, 1f, 1f) else Triple(0.15f, 0.08f, 0.20f)
                acc.addQuad(l0.x, l0.y + 0.03f, l0.z, r0.x, r0.y + 0.03f, r0.z, r1.x, r1.y + 0.03f, r1.z, l1.x, l1.y + 0.03f, l1.z, fr, fg, fb)
            } else {
                // Main road + center candy ribbon stripe
                val mL0 = n0.center - n0.right * (hw0 * 0.14f)
                val mR0 = n0.center + n0.right * (hw0 * 0.14f)
                val mL1 = n1.center - n1.right * (hw1 * 0.14f)
                val mR1 = n1.center + n1.right * (hw1 * 0.14f)

                val shade = if (i % 2 == 0) 1.0f else 0.93f
                acc.addQuad(l0.x, l0.y, l0.z, mL0.x, mL0.y, mL0.z, mL1.x, mL1.y, mL1.z, l1.x, l1.y, l1.z, rr * shade, rg * shade, rb * shade)
                acc.addQuad(mR0.x, mR0.y, mR0.z, r0.x, r0.y, r0.z, r1.x, r1.y, r1.z, mR1.x, mR1.y, mR1.z, rr * shade, rg * shade, rb * shade)

                val stripeAlt = if ((i / 2) % 2 == 0) 1f else 0.78f
                acc.addQuad(mL0.x, mL0.y + 0.015f, mL0.z, mR0.x, mR0.y + 0.015f, mR0.z, mR1.x, mR1.y + 0.015f, mR1.z, mL1.x, mL1.y + 0.015f, mL1.z, sr * stripeAlt, sg * stripeAlt, sb * stripeAlt)
            }

            // Elevated candy curbs & safety walls on both sides
            val isAlt = (i % 2 == 0)
            val cr = if (isAlt) c1r else c2r
            val cg = if (isAlt) c1g else c2g
            val cb = if (isAlt) c1b else c2b
            val curbW = 1.25f
            val curbH = 0.52f

            // Left curb
            val lcOut0 = l0 - n0.right * curbW
            val lcOut1 = l1 - n1.right * curbW
            acc.addQuad(l0.x, l0.y + curbH, l0.z, l0.x, l0.y, l0.z, l1.x, l1.y, l1.z, l1.x, l1.y + curbH, l1.z, cr, cg, cb)
            acc.addQuad(lcOut0.x, lcOut0.y + curbH, lcOut0.z, l0.x, l0.y + curbH, l0.z, l1.x, l1.y + curbH, l1.z, lcOut1.x, lcOut1.y + curbH, lcOut1.z, cr, cg, cb)

            // Right curb
            val rcOut0 = r0 + n0.right * curbW
            val rcOut1 = r1 + n1.right * curbW
            acc.addQuad(r0.x, r0.y, r0.z, r0.x, r0.y + curbH, r0.z, r1.x, r1.y + curbH, r1.z, r1.x, r1.y, r1.z, cr, cg, cb)
            acc.addQuad(r0.x, r0.y + curbH, r0.z, rcOut0.x, rcOut0.y + curbH, rcOut0.z, rcOut1.x, rcOut1.y + curbH, rcOut1.z, r1.x, r1.y + curbH, r1.z, cr, cg, cb)

            // Support skirt underneath elevated bridges
            if (n0.center.y > 0.3f || n1.center.y > 0.3f) {
                acc.addQuad(lcOut0.x, lcOut0.y + curbH, lcOut0.z, lcOut1.x, lcOut1.y + curbH, lcOut1.z, lcOut1.x, 0f, lcOut1.z, lcOut0.x, 0f, lcOut0.z, 0.72f, 0.45f, 0.25f)
                acc.addQuad(rcOut1.x, rcOut1.y + curbH, rcOut1.z, rcOut0.x, rcOut0.y + curbH, rcOut0.z, rcOut0.x, 0f, rcOut0.z, rcOut1.x, 0f, rcOut1.z, 0.72f, 0.45f, 0.25f)
            }
        }

        // Start/Finish Banner Arch at node 0
        val sn = track.nodes[0]
        val hw = sn.halfWidth + 1.2f
        val leftPillar = sn.center - sn.right * hw
        val rightPillar = sn.center + sn.right * hw
        acc.addCylinder(leftPillar.x, leftPillar.y, leftPillar.z, 0.9f, 0.9f, 10.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
        acc.addCylinder(rightPillar.x, rightPillar.y, rightPillar.z, 0.9f, 0.9f, 10.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
        // Overhead checkered candy beam
        val bL = leftPillar + Vec3(0f, 9.2f, 0f)
        val bR = rightPillar + Vec3(0f, 9.2f, 0f)
        val segs = 12
        for (s in 0 until segs) {
            val t0 = s.toFloat() / segs
            val t1 = (s + 1).toFloat() / segs
            val p0 = bL * (1f - t0) + bR * t0
            val p1 = bL * (1f - t1) + bR * t1
            val (br, bg, bb) = if (s % 2 == 0) Triple(1f, 0.22f, 0.55f) else Triple(1f, 0.96f, 0.98f)
            acc.addQuad(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p1.x, p1.y + 2.2f, p1.z, p0.x, p0.y + 2.2f, p0.z, br, bg, bb)
        }

        return acc.build()
    }

    fun buildGroundMesh(trackId: TrackId): GpuMesh {
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
                acc.addQuad(
                    x0, -0.15f, z1,
                    x1, -0.15f, z1,
                    x1, -0.15f, z0,
                    x0, -0.15f, z0,
                    gr * alt, gg * alt, gb * alt
                )
            }
        }
        return acc.build()
    }

    fun buildKartAndRacerMesh(
        kart: KartSpec,
        racer: RacerSpec,
        custom: KartCustomization
    ): GpuMesh {
        val acc = MeshAccumulator()
        val paint = CatalogData.paintById(custom.paintId)
        val primary = if (custom.paintId == "default") kart.defaultPrimaryHex else paint.primaryHex
        val secondary = if (custom.paintId == "default") kart.defaultSecondaryHex else paint.secondaryHex
        val accent = kart.accentHex

        // Distinct 3D geometry per kart body style!
        when (kart.style) {
            KartBodyStyle.STRAWBERRY_ROCKET -> {
                acc.addBox(0f, 0.58f, -0.1f, 1.35f, 0.42f, 2.3f, primary)
                acc.addCylinder(0f, 0.58f, 1.05f, 0.55f, 0.18f, 0.8f, 10, secondary) // nose cone
                acc.addSphere(0f, 0.65f, 1.35f, 0.52f, 0.28f, 0.72f, 6, 10, primary, secondary)
                acc.addBox(-0.78f, 0.65f, -0.45f, 0.16f, 0.48f, 1.15f, secondary) // left fin
                acc.addBox(0.78f, 0.65f, -0.45f, 0.16f, 0.48f, 1.15f, secondary)  // right fin
                acc.addBox(0f, 1.12f, -1.18f, 1.45f, 0.12f, 0.42f, accent) // rear wing
            }
            KartBodyStyle.CHOCOLATE_CRUISER -> {
                acc.addBox(0f, 0.64f, 0f, 1.65f, 0.54f, 2.75f, primary)
                acc.addBox(0f, 0.92f, 0.65f, 1.25f, 0.18f, 1.1f, secondary) // wafer hood
                acc.addBox(0f, 0.48f, 1.42f, 1.68f, 0.24f, 0.28f, secondary) // heavy bumper
                acc.addCylinder(-0.55f, 0.78f, -1.25f, 0.14f, 0.14f, 0.55f, 8, accent)
                acc.addCylinder(0.55f, 0.78f, -1.25f, 0.14f, 0.14f, 0.55f, 8, accent)
            }
            KartBodyStyle.PEPPERMINT_RACER -> {
                acc.addBox(0f, 0.52f, 0f, 1.05f, 0.36f, 2.8f, primary, 1f)
                acc.addBox(0f, 0.46f, 1.42f, 1.65f, 0.10f, 0.38f, secondary) // front formula wing
                acc.addSphere(0f, 0.62f, 0.95f, 0.44f, 0.28f, 0.68f, 6, 10, secondary, primary)
                acc.addBox(0f, 1.16f, -1.22f, 1.55f, 0.12f, 0.40f, secondary)
                acc.addBox(-0.42f, 0.85f, -1.22f, 0.10f, 0.48f, 0.32f, accent)
                acc.addBox(0.42f, 0.85f, -1.22f, 0.10f, 0.48f, 0.32f, accent)
            }
            KartBodyStyle.JELLYBEAN_MACHINE -> {
                acc.addSphere(0f, 0.68f, 0.05f, 0.92f, 0.48f, 1.48f, 8, 12, primary, secondary)
                acc.addBox(0f, 0.45f, 0f, 1.42f, 0.22f, 2.2f, secondary)
                acc.addCylinder(-0.44f, 0.82f, -1.1f, 0.18f, 0.16f, 0.5f, 8, accent)
                acc.addCylinder(0.44f, 0.82f, -1.1f, 0.18f, 0.16f, 0.5f, 8, accent)
            }
            KartBodyStyle.CUPCAKE_KART -> {
                acc.addCylinder(0f, 0.34f, 0f, 0.82f, 1.04f, 0.62f, 14, primary, secondary) // fluted wrapper
                acc.addSphere(0f, 0.92f, 0.25f, 0.92f, 0.36f, 0.98f, 6, 12, secondary, 0xFFFFFFFF) // frosting
                acc.addSphere(0f, 1.24f, -0.75f, 0.24f, 0.24f, 0.24f, 6, 8, accent) // cherry spoiler
            }
            KartBodyStyle.DONUT_DRIFTER -> {
                acc.addTorus(0f, 0.64f, 0f, 0.88f, 0.38f, 16, 8, primary, secondary)
                acc.addBox(0f, 0.46f, 0f, 1.35f, 0.20f, 2.25f, accent)
                acc.addBox(0f, 1.05f, -1.18f, 1.45f, 0.12f, 0.38f, secondary)
            }
            KartBodyStyle.MARSHMALLOW_SPEEDSTER -> {
                acc.addCylinder(-0.56f, 0.56f, -0.2f, 0.38f, 0.38f, 1.9f, 10, primary)
                acc.addCylinder(0.56f, 0.56f, -0.2f, 0.38f, 0.38f, 1.9f, 10, primary)
                acc.addBox(0f, 0.52f, 0f, 1.35f, 0.26f, 2.4f, secondary)
                acc.addSphere(0f, 0.70f, 1.15f, 0.68f, 0.34f, 0.50f, 6, 10, primary, accent)
            }
            KartBodyStyle.CARAMEL_THUNDER -> {
                acc.addBox(0f, 0.56f, 0f, 1.55f, 0.38f, 2.85f, primary)
                acc.addBox(0f, 0.72f, 0.55f, 1.15f, 0.24f, 1.2f, secondary)
                acc.addBox(-0.78f, 0.62f, 0.2f, 0.24f, 0.32f, 1.9f, accent)
                acc.addBox(0.78f, 0.62f, 0.2f, 0.24f, 0.32f, 1.9f, accent)
                acc.addBox(0f, 1.18f, -1.28f, 1.72f, 0.14f, 0.45f, accent)
            }
        }

        // Steering wheel inside cockpit
        acc.addTorus(0f, 1.02f, 0.48f, 0.22f, 0.045f, 10, 6, 0xFF222222, 0xFFFFFFFF, verticalArch = true)

        // Optional Kart Decoration
        when (custom.decoration) {
            KartDecoration.NONE -> Unit
            KartDecoration.CHERRY_TOPPER -> {
                acc.addCylinder(0f, 0.9f, -1.15f, 0.05f, 0.05f, 0.55f, 6, 0xFF2ECC71)
                acc.addSphere(0f, 1.48f, -1.15f, 0.22f, 0.22f, 0.22f, 6, 8, 0xFFFF1744)
            }
            KartDecoration.CANDY_CANE_WING -> {
                acc.addBox(0f, 1.25f, -1.2f, 1.65f, 0.14f, 0.36f, 0xFFFF2448)
                acc.addBox(0f, 1.33f, -1.2f, 1.2f, 0.10f, 0.36f, 0xFFFFFFFF)
            }
            KartDecoration.LOLLIPOP_ANTENNA -> {
                acc.addCylinder(-0.55f, 0.9f, -1.1f, 0.04f, 0.04f, 0.75f, 6, 0xFFFFFFFF)
                acc.addSphere(-0.55f, 1.72f, -1.1f, 0.26f, 0.26f, 0.10f, 6, 10, 0xFFFF3D8B, 0xFFFFFFFF)
            }
            KartDecoration.CROWN_SPRINKLE -> {
                acc.addCylinder(0f, 1.78f, -0.08f, 0.20f, 0.28f, 0.22f, 8, 0xFFFFD700, 0xFFFF9800)
            }
        }

        // Exhaust pipes
        val exhaustColor = custom.exhaustStyle.flameHex
        acc.addCylinder(-0.32f, 0.62f, -1.42f, 0.12f, 0.15f, 0.35f, 8, 0xFF444444, exhaustColor)
        acc.addCylinder(0.32f, 0.62f, -1.42f, 0.12f, 0.15f, 0.35f, 8, 0xFF444444, exhaustColor)

        // Seated Racer Character inside the kart!
        // Torso
        acc.addBox(0f, 1.05f, -0.08f, 0.56f, 0.54f, 0.40f, racer.suitPrimaryHex)
        // Racing stripe / scarf
        acc.addBox(0f, 1.08f, -0.10f, 0.22f, 0.52f, 0.43f, racer.suitSecondaryHex)
        // Left & right arms reaching to steering wheel + gloves
        acc.addBox(-0.34f, 1.08f, 0.16f, 0.15f, 0.15f, 0.44f, racer.suitPrimaryHex)
        acc.addBox(0.34f, 1.08f, 0.16f, 0.15f, 0.15f, 0.44f, racer.suitPrimaryHex)
        acc.addSphere(-0.26f, 1.04f, 0.42f, 0.11f, 0.11f, 0.11f, 5, 6, racer.suitSecondaryHex)
        acc.addSphere(0.26f, 1.04f, 0.42f, 0.11f, 0.11f, 0.11f, 5, 6, racer.suitSecondaryHex)
        // Helmet & Visor
        acc.addSphere(0f, 1.54f, -0.05f, 0.34f, 0.33f, 0.36f, 7, 10, racer.helmetHex, racer.suitSecondaryHex)
        acc.addBox(0f, 1.53f, 0.22f, 0.44f, 0.18f, 0.20f, racer.visorHex)

        return acc.build()
    }

    fun buildWheelMesh(design: WheelDesign): GpuMesh {
        val acc = MeshAccumulator()
        acc.addHorizontalWheelCylinder(
            cx = 0f, cy = 0f, cz = 0f,
            radius = 0.44f, width = 0.34f, segs = 12,
            tireHex = 0xFF28192D,
            rimHex = design.primaryHex,
            altRimHex = design.rimHex
        )
        return acc.build()
    }

    fun buildBoostPadMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0.04f, 0f, 3.6f, 0.08f, 4.4f, 0xFFFF3D8B)
        acc.addBox(0f, 0.07f, 0.6f, 2.4f, 0.08f, 0.9f, 0xFFFFEB3B)
        acc.addBox(0f, 0.07f, -0.8f, 2.4f, 0.08f, 0.9f, 0xFF3CE8B5)
        return acc.build()
    }

    fun buildJumpRampMesh(): GpuMesh {
        val acc = MeshAccumulator()
        val r = 1f; val g = 0.58f; val b = 0.12f
        val hw = 2.6f; val hl = 2.6f; val h = 1.15f
        // Wedge ramp sloping up along +Z
        acc.addQuad(-hw, 0f, -hl, hw, 0f, -hl, hw, h, hl, -hw, h, hl, r, g, b)
        acc.addQuad(-hw, 0f, hl, hw, 0f, hl, hw, h, hl, -hw, h, hl, 1f, 0.25f, 0.55f)
        acc.addTri(-hw, 0f, -hl, -hw, 0f, hl, -hw, h, hl, 1f, 0.9f, 0.3f)
        acc.addTri(hw, 0f, -hl, hw, h, hl, hw, 0f, hl, 1f, 0.9f, 0.3f)
        return acc.build()
    }

    fun buildHazardMesh(isCaramel: Boolean): GpuMesh {
        val acc = MeshAccumulator()
        if (isCaramel) {
            acc.addCylinder(0f, 0.02f, 0f, 2.2f, 2.1f, 0.12f, 12, 0xFFFF9800, 0xFFFFB74D)
            acc.addSphere(0.5f, 0.18f, 0.3f, 0.35f, 0.22f, 0.35f, 5, 6, 0xFFFFCC80)
        } else {
            acc.addCylinder(0f, 0.02f, 0f, 1.55f, 1.55f, 0.32f, 12, 0xFFFF2448, 0xFFFFFFFF)
        }
        return acc.build()
    }

    fun buildItemBoxMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0f, 0f, 1.45f, 1.45f, 1.45f, 0xFFFFEB3B, 0.90f)
        acc.addBox(0f, 0f, 0f, 1.56f, 0.35f, 1.56f, 0xFFFF3D8B, 0.95f)
        acc.addBox(0f, 0f, 0f, 0.35f, 1.56f, 1.56f, 0xFF3CE8B5, 0.95f)
        return acc.build()
    }

    fun buildSimpleSphereMesh(hex: Long, alpha: Float): GpuMesh {
        val acc = MeshAccumulator()
        acc.addSphere(0f, 0f, 0f, 1f, 1f, 1f, 8, 12, hex, hex, alpha)
        return acc.build()
    }

    fun buildSimpleCubeMesh(): GpuMesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0f, 0f, 1f, 1f, 1f, 0xFFFFFFFF)
        return acc.build()
    }

    fun buildDecorationMesh(type: DecoType, c1: Long, c2: Long): GpuMesh {
        val acc = MeshAccumulator()
        when (type) {
            DecoType.LOLLIPOP_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.35f, 0.30f, 6.2f, 8, 0xFFFFFFFF)
                acc.addSphere(0f, 7.6f, 0f, 2.4f, 2.4f, 0.85f, 8, 12, c1, 0xFFFFFFFF)
                acc.addTorus(0f, 7.6f, 0f, 2.35f, 0.32f, 14, 6, c2, c2, verticalArch = true)
            }
            DecoType.GUMDROP_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.45f, 0.38f, 3.2f, 8, 0xFF6D4C41)
                acc.addCylinder(0f, 3.0f, 0f, 2.2f, 0.4f, 3.8f, 10, c1, c2)
            }
            DecoType.GUMDROP_HILL -> {
                acc.addCylinder(0f, 0f, 0f, 6.5f, 2.2f, 7.2f, 12, c1)
                acc.addSphere(0f, 7.0f, 0f, 2.3f, 1.8f, 2.3f, 6, 10, c1, c2)
            }
            DecoType.MARSHMALLOW_BUSH -> {
                acc.addCylinder(-0.9f, 0f, 0f, 1.1f, 1.1f, 1.6f, 10, 0xFFFFF8FF)
                acc.addCylinder(0.9f, 0f, 0.4f, 1.0f, 1.0f, 1.9f, 10, c1)
                acc.addCylinder(0f, 0f, -0.7f, 1.1f, 1.1f, 2.2f, 10, 0xFFFFF8FF)
            }
            DecoType.COTTON_CANDY_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.32f, 0.28f, 4.5f, 8, 0xFFFFE0B2)
                acc.addSphere(0f, 5.8f, 0f, 2.6f, 2.2f, 2.6f, 7, 10, c1)
                acc.addSphere(1.4f, 5.1f, 0.9f, 1.6f, 1.5f, 1.6f, 6, 8, c2)
                acc.addSphere(-1.3f, 5.2f, -0.8f, 1.5f, 1.4f, 1.5f, 6, 8, 0xFFFFB8DE)
            }
            DecoType.CANDY_CANE_PLANT -> {
                acc.addCylinder(0f, 0f, 0f, 0.34f, 0.34f, 4.4f, 8, 0xFFFF2448, 0xFFFFFFFF)
                acc.addTorus(0.9f, 4.4f, 0f, 0.9f, 0.34f, 10, 6, 0xFFFF2448, 0xFFFFFFFF, verticalArch = true)
            }
            DecoType.CHOCOLATE_TREE -> {
                acc.addCylinder(0f, 0f, 0f, 0.55f, 0.35f, 4.8f, 8, 0xFF4E2A1E)
                acc.addSphere(0f, 5.6f, 0f, 2.5f, 1.8f, 2.5f, 7, 10, 0xFF6D3B22, c1)
            }
            DecoType.GINGERBREAD_HOUSE -> {
                acc.addBox(0f, 2.4f, 0f, 6.2f, 4.8f, 5.6f, 0xFFB86F3A)
                // Frosting roof
                acc.addCylinder(0f, 4.8f, 0f, 4.9f, 0.2f, 3.2f, 4, 0xFFFFF8FF, c1)
                // Candy door & trim
                acc.addBox(0f, 1.4f, 2.85f, 1.5f, 2.6f, 0.25f, c2)
                acc.addSphere(-1.8f, 2.6f, 2.85f, 0.55f, 0.55f, 0.2f, 5, 8, c1)
                acc.addSphere(1.8f, 2.6f, 2.85f, 0.55f, 0.55f, 0.2f, 5, 8, c1)
            }
            DecoType.CUPCAKE_TOWER -> {
                acc.addCylinder(0f, 0f, 0f, 3.2f, 4.1f, 5.4f, 14, c1, 0xFFFFE0EC)
                acc.addSphere(0f, 6.4f, 0f, 4.2f, 2.6f, 4.2f, 7, 12, 0xFFFFF8FF, c2)
                acc.addSphere(0f, 9.2f, 0f, 1.0f, 1.0f, 1.0f, 6, 8, 0xFFFF1744) // giant cherry
            }
            DecoType.COOKIE_HOUSE -> {
                acc.addBox(0f, 2.0f, 0f, 5.8f, 4.0f, 5.8f, 0xFFD99B52)
                acc.addBox(0f, 4.2f, 0f, 6.4f, 0.8f, 6.4f, 0xFF4E2A1E)
                acc.addSphere(0f, 2.0f, 2.95f, 0.9f, 0.9f, 0.3f, 5, 8, c1)
            }
            DecoType.CHOCOLATE_CASTLE -> {
                acc.addBox(0f, 4.0f, 0f, 8.5f, 8.0f, 8.5f, 0xFF4A2518)
                for (sx in listOf(-4.0f, 4.0f)) {
                    for (sz in listOf(-4.0f, 4.0f)) {
                        acc.addCylinder(sx, 0f, sz, 1.4f, 1.3f, 10.5f, 10, 0xFF6B3A26)
                        acc.addCylinder(sx, 10.5f, sz, 1.7f, 0.1f, 2.6f, 10, c1)
                    }
                }
            }
            DecoType.DONUT_BUILDING -> {
                acc.addCylinder(0f, 0f, 0f, 3.6f, 3.6f, 2.5f, 12, 0xFFF4B678)
                acc.addTorus(0f, 4.8f, 0f, 2.8f, 1.25f, 16, 8, 0xFFF4B678, c1, verticalArch = true)
            }
            DecoType.GUMMY_BEAR_STATUE -> {
                // Cute giant gummy bear monument!
                acc.addSphere(0f, 2.6f, 0f, 2.1f, 2.6f, 1.7f, 7, 10, c1) // torso
                acc.addSphere(0f, 5.6f, 0f, 1.7f, 1.6f, 1.5f, 7, 10, c1) // head
                acc.addSphere(-1.3f, 7.0f, 0f, 0.65f, 0.65f, 0.45f, 5, 8, c1) // L ear
                acc.addSphere(1.3f, 7.0f, 0f, 0.65f, 0.65f, 0.45f, 5, 8, c1)  // R ear
                acc.addSphere(0f, 5.3f, 1.35f, 0.75f, 0.55f, 0.55f, 5, 8, c2) // snout
                acc.addSphere(-1.9f, 3.2f, 0.6f, 0.7f, 1.1f, 0.7f, 5, 8, c1)
                acc.addSphere(1.9f, 3.2f, 0.6f, 0.7f, 1.1f, 0.7f, 5, 8, c1)
            }
            DecoType.SUGAR_CRYSTAL -> {
                acc.addCylinder(0f, 0f, 0f, 1.2f, 0.08f, 5.8f, 6, c1, 0xFFFFFFFF)
                acc.addCylinder(1.2f, 0f, 0.6f, 0.8f, 0.05f, 3.9f, 6, c2, 0xFFFFFFFF)
                acc.addCylinder(-1.1f, 0f, -0.5f, 0.9f, 0.05f, 4.2f, 6, c1)
            }
            DecoType.FLOATING_BALLOON -> {
                acc.addSphere(0f, 0f, 0f, 2.2f, 2.6f, 2.2f, 8, 12, c1, c2)
                acc.addCylinder(0f, -3.5f, 0f, 0.06f, 0.06f, 1.2f, 5, 0xFFFFFFFF)
            }
            DecoType.CHOCOLATE_WATERFALL -> {
                acc.addBox(0f, 6.0f, -2.0f, 7.0f, 12.0f, 4.0f, 0xFF4A2315)
                acc.addBox(0f, 6.0f, 0.3f, 4.2f, 11.8f, 0.8f, 0xFF7B3F1D)
                acc.addCylinder(0f, 0f, 1.8f, 4.5f, 4.5f, 0.5f, 12, 0xFF8C4A27, 0xFFFFB74D)
            }
            DecoType.STRAWBERRY_FOUNTAIN -> {
                acc.addCylinder(0f, 0f, 0f, 4.6f, 4.2f, 1.2f, 14, 0xFFFFF8FF)
                acc.addCylinder(0f, 1.0f, 0f, 1.1f, 0.9f, 3.6f, 10, 0xFFFF6EB4)
                acc.addCylinder(0f, 3.8f, 0f, 2.4f, 2.6f, 0.8f, 12, 0xFFFF3D8B)
            }
            DecoType.CANDY_LAMP -> {
                acc.addCylinder(0f, 0f, 0f, 0.22f, 0.16f, 4.8f, 8, 0xFFFFFFFF, 0xFFFF3D8B)
                acc.addSphere(0f, 5.2f, 0f, 0.72f, 0.72f, 0.72f, 6, 8, c1)
            }
            DecoType.CANDY_CANE_ARCH -> {
                acc.addCylinder(-14.2f, 0f, 0f, 0.9f, 0.9f, 7.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
                acc.addCylinder(14.2f, 0f, 0f, 0.9f, 0.9f, 7.5f, 10, 0xFFFF2448, 0xFFFFFFFF)
                // Curved candy arch top
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
                acc.addTorus(0f, 4.5f, 0f, 14.5f, 1.55f, 18, 8, 0xFFF4B678, c1, verticalArch = true)
            }
            DecoType.WAFER_TUNNEL_RING -> {
                acc.addBox(-14.2f, 5.0f, 0f, 1.4f, 10.0f, 7.0f, c1)
                acc.addBox(14.2f, 5.0f, 0f, 1.4f, 10.0f, 7.0f, c1)
                acc.addBox(0f, 10.4f, 0f, 29.8f, 1.5f, 7.0f, c2)
            }
        }
        return acc.build()
    }
}
