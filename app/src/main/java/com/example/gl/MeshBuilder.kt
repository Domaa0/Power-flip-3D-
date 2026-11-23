package com.example.gl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MeshGeometry(
    val buffer: FloatBuffer,
    val vertexCount: Int
)

object MeshBuilder {

    private fun toFloatBuffer(list: List<Float>): MeshGeometry {
        val arr = list.toFloatArray()
        val bb = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(arr)
        fb.position(0)
        return MeshGeometry(fb, arr.size / 6)
    }

    private fun pushVertex(out: MutableList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        out.add(x)
        out.add(y)
        out.add(z)
        out.add(nx)
        out.add(ny)
        out.add(nz)
    }

    private fun pushQuad(
        out: MutableList<Float>,
        ax: Float, ay: Float, az: Float, anx: Float, any: Float, anz: Float,
        bx: Float, by: Float, bz: Float, bnx: Float, bny: Float, bnz: Float,
        cx: Float, cy: Float, cz: Float, cnx: Float, cny: Float, cnz: Float,
        dx: Float, dy: Float, dz: Float, dnx: Float, dny: Float, dnz: Float
    ) {
        pushVertex(out, ax, ay, az, anx, any, anz)
        pushVertex(out, bx, by, bz, bnx, bny, bnz)
        pushVertex(out, cx, cy, cz, cnx, cny, cnz)

        pushVertex(out, ax, ay, az, anx, any, anz)
        pushVertex(out, cx, cy, cz, cnx, cny, cnz)
        pushVertex(out, dx, dy, dz, dnx, dny, dnz)
    }

    fun createUnitBox(): MeshGeometry {
        val out = mutableListOf<Float>()
        val h = 0.5f
        pushQuad(out, -h,-h, h, 0f,0f,1f,   h,-h, h, 0f,0f,1f,   h, h, h, 0f,0f,1f,  -h, h, h, 0f,0f,1f)
        pushQuad(out,  h,-h,-h, 0f,0f,-1f, -h,-h,-h, 0f,0f,-1f, -h, h,-h, 0f,0f,-1f,  h, h,-h, 0f,0f,-1f)
        pushQuad(out,  h,-h, h, 1f,0f,0f,   h,-h,-h, 1f,0f,0f,   h, h,-h, 1f,0f,0f,   h, h, h, 1f,0f,0f)
        pushQuad(out, -h,-h,-h,-1f,0f,0f,  -h,-h, h,-1f,0f,0f,  -h, h, h,-1f,0f,0f,  -h, h,-h,-1f,0f,0f)
        pushQuad(out, -h, h, h, 0f,1f,0f,   h, h, h, 0f,1f,0f,   h, h,-h, 0f,1f,0f,  -h, h,-h, 0f,1f,0f)
        pushQuad(out, -h,-h,-h, 0f,-1f,0f,  h,-h,-h, 0f,-1f,0f,  h,-h, h, 0f,-1f,0f, -h,-h, h, 0f,-1f,0f)
        return toFloatBuffer(out)
    }

    fun createUnitCylinder(slices: Int = 24): MeshGeometry {
        val out = mutableListOf<Float>()
        val h = 0.5f
        for (i in 0 until slices) {
            val a0 = (2.0 * PI * i / slices).toFloat()
            val a1 = (2.0 * PI * (i + 1) / slices).toFloat()
            val x0 = cos(a0)
            val z0 = sin(a0)
            val x1 = cos(a1)
            val z1 = sin(a1)

            pushQuad(
                out,
                x0, -h, z0, x0, 0f, z0,
                x1, -h, z1, x1, 0f, z1,
                x1,  h, z1, x1, 0f, z1,
                x0,  h, z0, x0, 0f, z0
            )
            pushVertex(out, 0f, h, 0f, 0f, 1f, 0f)
            pushVertex(out, x1, h, z1, 0f, 1f, 0f)
            pushVertex(out, x0, h, z0, 0f, 1f, 0f)

            pushVertex(out, 0f, -h, 0f, 0f, -1f, 0f)
            pushVertex(out, x0, -h, z0, 0f, -1f, 0f)
            pushVertex(out, x1, -h, z1, 0f, -1f, 0f)
        }
        return toFloatBuffer(out)
    }

    fun createFloorQuad(): MeshGeometry {
        val out = mutableListOf<Float>()
        val s = 0.5f
        pushQuad(
            out,
            -s, 0f,  s, 0f, 1f, 0f,
             s, 0f,  s, 0f, 1f, 0f,
             s, 0f, -s, 0f, 1f, 0f,
            -s, 0f, -s, 0f, 1f, 0f
        )
        return toFloatBuffer(out)
    }

    // --- PENIS SKIN MESHES ---

    fun createHeroBaseLobes(): MeshGeometry {
        val out = mutableListOf<Float>()
        appendSphere(out, cx = -0.04f, cy = 0.23f, cz = -0.20f, radius = 0.235f, stacks = 16, slices = 22)
        appendSphere(out, cx = -0.04f, cy = 0.23f, cz =  0.20f, radius = 0.235f, stacks = 16, slices = 22)
        return toFloatBuffer(out)
    }

    fun createHeroShaft(): MeshGeometry {
        val profile = listOf(
            0.12f to 0.00f,
            0.13f to 0.16f,
            0.22f to 0.225f,
            0.46f to 0.230f,
            0.78f to 0.220f,
            1.06f to 0.208f,
            1.22f to 0.195f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0.03f)
        return toFloatBuffer(out)
    }

    fun createHeroCollar(): MeshGeometry {
        val profile = listOf(
            1.19f to 0.195f,
            1.23f to 0.232f,
            1.27f to 0.242f,
            1.31f to 0.218f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0.03f)
        return toFloatBuffer(out)
    }

    fun createHeroCap(): MeshGeometry {
        val profile = listOf(
            1.28f to 0.215f,
            1.33f to 0.242f,
            1.42f to 0.238f,
            1.52f to 0.212f,
            1.61f to 0.162f,
            1.67f to 0.095f,
            1.70f to 0.00f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0.03f)
        return toFloatBuffer(out)
    }

    fun createHeroEyeWhites(): MeshGeometry {
        val out = mutableListOf<Float>()
        appendSphere(out, cx = 0.21f, cy = 1.00f, cz = -0.085f, radius = 0.048f, stacks = 10, slices = 12)
        appendSphere(out, cx = 0.21f, cy = 1.00f, cz =  0.085f, radius = 0.048f, stacks = 10, slices = 12)
        return toFloatBuffer(out)
    }

    fun createHeroEyePupils(): MeshGeometry {
        val out = mutableListOf<Float>()
        appendSphere(out, cx = 0.248f, cy = 1.005f, cz = -0.085f, radius = 0.025f, stacks = 8, slices = 10)
        appendSphere(out, cx = 0.248f, cy = 1.005f, cz =  0.085f, radius = 0.025f, stacks = 8, slices = 10)
        return toFloatBuffer(out)
    }

    // --- PUSSY SKIN MESHES (stylized 3D vinyl toy silhouette facing camera +Z) ---

    fun createPussyOuterBody(): MeshGeometry {
        val out = mutableListOf<Float>()
        // Flat resting base pad so it stands neatly on platforms
        val basePad = listOf(
            0.00f to 0.00f,
            0.00f to 0.26f,
            0.08f to 0.30f,
            0.16f to 0.26f,
            0.20f to 0.00f
        )
        appendLathe(out, basePad, slices = 24, xOffset = 0f)

        // Left & right plump sculpted outer cushion lobes forming an unmistakable peach/heart contour with central cleft
        appendEllipsoid(
            out,
            cx = -0.175f, cy = 0.78f, cz = 0.0f,
            rx = 0.255f, ry = 0.72f, rz = 0.28f,
            stacks = 18, slices = 22
        )
        appendEllipsoid(
            out,
            cx = 0.175f, cy = 0.78f, cz = 0.0f,
            rx = 0.255f, ry = 0.72f, rz = 0.28f,
            stacks = 18, slices = 22
        )
        return toFloatBuffer(out)
    }

    fun createPussyInnerPetals(): MeshGeometry {
        val out = mutableListOf<Float>()
        // Inner sculpted petals tucked cleanly inside the central vertical cleft facing +Z
        appendEllipsoid(
            out,
            cx = -0.062f, cy = 0.74f, cz = 0.21f,
            rx = 0.075f, ry = 0.44f, rz = 0.10f,
            stacks = 14, slices = 16
        )
        appendEllipsoid(
            out,
            cx = 0.062f, cy = 0.74f, cz = 0.21f,
            rx = 0.075f, ry = 0.44f, rz = 0.10f,
            stacks = 14, slices = 16
        )
        return toFloatBuffer(out)
    }

    fun createPussyCrest(): MeshGeometry {
        val out = mutableListOf<Float>()
        // Glossy top pearl/crest bead at the top of the inner cleft
        appendSphere(out, cx = 0f, cy = 1.16f, cz = 0.26f, radius = 0.078f, stacks = 12, slices = 16)
        // Subtle darker inner seam core
        appendEllipsoid(
            out,
            cx = 0f, cy = 0.70f, cz = 0.225f,
            rx = 0.032f, ry = 0.36f, rz = 0.055f,
            stacks = 10, slices = 12
        )
        return toFloatBuffer(out)
    }

    fun createPussyEyeWhites(): MeshGeometry {
        val out = mutableListOf<Float>()
        appendSphere(out, cx = -0.17f, cy = 1.24f, cz = 0.21f, radius = 0.052f, stacks = 10, slices = 12)
        appendSphere(out, cx =  0.17f, cy = 1.24f, cz = 0.21f, radius = 0.052f, stacks = 10, slices = 12)
        return toFloatBuffer(out)
    }

    fun createPussyEyePupils(): MeshGeometry {
        val out = mutableListOf<Float>()
        appendSphere(out, cx = -0.17f, cy = 1.245f, cz = 0.250f, radius = 0.027f, stacks = 8, slices = 10)
        appendSphere(out, cx =  0.17f, cy = 1.245f, cz = 0.250f, radius = 0.027f, stacks = 8, slices = 10)
        return toFloatBuffer(out)
    }

    // --- WATER BOTTLE MESHES ---

    fun createWaterBottleBody(): MeshGeometry {
        val profile = listOf(
            0.00f to 0.00f,
            0.00f to 0.25f,
            0.05f to 0.29f,
            0.22f to 0.295f,
            0.28f to 0.275f,
            0.34f to 0.295f,
            0.44f to 0.27f,
            0.52f to 0.285f,
            1.02f to 0.285f,
            1.14f to 0.265f,
            1.28f to 0.17f,
            1.38f to 0.11f,
            1.44f to 0.105f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0f)
        return toFloatBuffer(out)
    }

    fun createWaterBottleLabel(): MeshGeometry {
        val profile = listOf(
            0.56f to 0.289f,
            0.96f to 0.289f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0f)
        return toFloatBuffer(out)
    }

    fun createWaterBottleCap(): MeshGeometry {
        val profile = listOf(
            1.42f to 0.125f,
            1.44f to 0.132f,
            1.60f to 0.128f,
            1.62f to 0.115f,
            1.62f to 0.00f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 24, xOffset = 0f)
        return toFloatBuffer(out)
    }

    // --- SODA BOTTLE MESHES ---

    fun createSodaBottleBody(): MeshGeometry {
        val profile = listOf(
            0.00f to 0.00f,
            0.00f to 0.24f,
            0.06f to 0.28f,
            0.34f to 0.29f,
            0.54f to 0.24f,
            0.84f to 0.265f,
            1.04f to 0.24f,
            1.26f to 0.14f,
            1.46f to 0.095f,
            1.54f to 0.095f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0f)
        return toFloatBuffer(out)
    }

    fun createSodaBottleLabel(): MeshGeometry {
        val profile = listOf(
            0.60f to 0.258f,
            0.92f to 0.268f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0f)
        return toFloatBuffer(out)
    }

    fun createSodaBottleCap(): MeshGeometry {
        val profile = listOf(
            1.52f to 0.118f,
            1.55f to 0.128f,
            1.63f to 0.116f,
            1.64f to 0.00f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 20, xOffset = 0f)
        return toFloatBuffer(out)
    }

    // --- TROPHY / CHAMPAGNE BOTTLE MESHES ---

    fun createTrophyBottleBody(): MeshGeometry {
        val profile = listOf(
            0.00f to 0.00f,
            0.00f to 0.27f,
            0.05f to 0.305f,
            0.68f to 0.305f,
            0.95f to 0.265f,
            1.22f to 0.15f,
            1.46f to 0.105f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 28, xOffset = 0f)
        return toFloatBuffer(out)
    }

    fun createTrophyBottleFoil(): MeshGeometry {
        val profile = listOf(
            1.14f to 0.188f,
            1.46f to 0.112f,
            1.54f to 0.125f,
            1.64f to 0.118f,
            1.65f to 0.00f
        )
        val out = mutableListOf<Float>()
        appendLathe(out, profile, slices = 24, xOffset = 0f)
        return toFloatBuffer(out)
    }

    private fun appendSphere(
        out: MutableList<Float>,
        cx: Float,
        cy: Float,
        cz: Float,
        radius: Float,
        stacks: Int,
        slices: Int
    ) {
        appendEllipsoid(out, cx, cy, cz, radius, radius, radius, stacks, slices)
    }

    private fun appendEllipsoid(
        out: MutableList<Float>,
        cx: Float,
        cy: Float,
        cz: Float,
        rx: Float,
        ry: Float,
        rz: Float,
        stacks: Int,
        slices: Int
    ) {
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks).toFloat()
            val phi1 = (PI * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val theta0 = (2.0 * PI * j / slices).toFloat()
                val theta1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val u00x = sin(phi0) * cos(theta0)
                val u00y = cos(phi0)
                val u00z = sin(phi0) * sin(theta0)

                val u10x = sin(phi1) * cos(theta0)
                val u10y = cos(phi1)
                val u10z = sin(phi1) * sin(theta0)

                val u11x = sin(phi1) * cos(theta1)
                val u11y = cos(phi1)
                val u11z = sin(phi1) * sin(theta1)

                val u01x = sin(phi0) * cos(theta1)
                val u01y = cos(phi0)
                val u01z = sin(phi0) * sin(theta1)

                val (n00x, n00y, n00z) = normEllipsoid(u00x, u00y, u00z, rx, ry, rz)
                val (n10x, n10y, n10z) = normEllipsoid(u10x, u10y, u10z, rx, ry, rz)
                val (n11x, n11y, n11z) = normEllipsoid(u11x, u11y, u11z, rx, ry, rz)
                val (n01x, n01y, n01z) = normEllipsoid(u01x, u01y, u01z, rx, ry, rz)

                pushQuad(
                    out,
                    cx + u00x * rx, cy + u00y * ry, cz + u00z * rz, n00x, n00y, n00z,
                    cx + u10x * rx, cy + u10y * ry, cz + u10z * rz, n10x, n10y, n10z,
                    cx + u11x * rx, cy + u11y * ry, cz + u11z * rz, n11x, n11y, n11z,
                    cx + u01x * rx, cy + u01y * ry, cz + u01z * rz, n01x, n01y, n01z
                )
            }
        }
    }

    private fun normEllipsoid(ux: Float, uy: Float, uz: Float, rx: Float, ry: Float, rz: Float): Triple<Float, Float, Float> {
        val nx = ux / rx
        val ny = uy / ry
        val nz = uz / rz
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        return Triple(nx / len, ny / len, nz / len)
    }

    private fun appendLathe(
        out: MutableList<Float>,
        profile: List<Pair<Float, Float>>,
        slices: Int,
        xOffset: Float
    ) {
        val slopes = profile.indices.map { i ->
            val prev = profile[maxOf(0, i - 1)]
            val next = profile[minOf(profile.lastIndex, i + 1)]
            val dy = next.first - prev.first
            val dr = next.second - prev.second
            val nr = dy
            val ny = -dr
            val len = sqrt(nr * nr + ny * ny).coerceAtLeast(0.0001f)
            (nr / len) to (ny / len)
        }

        for (i in 0 until profile.lastIndex) {
            val (y0, r0) = profile[i]
            val (y1, r1) = profile[i + 1]
            val (nr0, ny0) = slopes[i]
            val (nr1, ny1) = slopes[i + 1]

            for (j in 0 until slices) {
                val a0 = (2.0 * PI * j / slices).toFloat()
                val a1 = (2.0 * PI * (j + 1) / slices).toFloat()
                val c0 = cos(a0)
                val s0 = sin(a0)
                val c1 = cos(a1)
                val s1 = sin(a1)

                pushQuad(
                    out,
                    xOffset + c0 * r0, y0, s0 * r0, c0 * nr0, ny0, s0 * nr0,
                    xOffset + c1 * r0, y0, s1 * r0, c1 * nr0, ny0, s1 * nr0,
                    xOffset + c1 * r1, y1, s1 * r1, c1 * nr1, ny1, s1 * nr1,
                    xOffset + c0 * r1, y1, s0 * r1, c0 * nr1, ny1, s0 * nr1
                )
            }
        }
    }
}
