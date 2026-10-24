package com.mercador.core.flight

/** Destino dos segmentos 2D (em pixels, origem no canto superior esquerdo, y para baixo). */
fun interface SegmentSink {
    fun segment(x0: Float, y0: Float, x1: Float, y1: Float)
}

/**
 * Projeta o wireframe de uma nave na tela, com a mesma lógica do LL9 do Elite (portado do eliteAndroid):
 *  0. PLUT: converte as coordenadas para a vista atual ([view]);
 *  1. faces visíveis = normal voltada para o observador (o jogador na origem);
 *  2. distância XX4 = z/128 (0..31) se z < 4096; acima disso XX4 = 31 e, se z/256 passar
 *     da "visibility distance" do modelo, a nave vira um ponto (SHPPT);
 *  3. aresta desenhada se sua visibilidade ≥ XX4 e uma de suas faces for visível;
 *  4. projeção x/z·focal + centro, com recorte no plano próximo.
 *
 * Campo de visão: o do BBC Elite na horizontal (meia largura = z/2, focal = largura); numa tela em
 * retrato a altura só amplia a visão vertical.
 */
class WireframeProjector {

    var viewportWidth = 256f
    var viewportHeight = 192f
    var nearZ = 1f
    var view = View.FRONT

    private val camModel = Vec3()
    private val viewPos = Vec3()
    private val tmp = Vec3()
    private var world = FloatArray(64 * 3)
    private var faceVisible = BooleanArray(32)

    val focal: Float get() = viewportWidth

    /** Projeta [ship] e envia os segmentos visíveis para [sink]. Retorna o nº de segmentos (−1 = virou ponto). */
    fun project(ship: ShipInstance, sink: SegmentSink): Int {
        val bp = ship.blueprint
        val pos = ship.position

        view.toView(pos.x, pos.y, pos.z, viewPos)
        // Descarta naves inteiramente atrás do plano próximo.
        if (viewPos.z + bp.boundingRadius < nearZ) return 0

        // LL9 parte 2: distância para o nível de detalhe, ou ponto se estiver muito longe.
        val xx4: Int
        if (viewPos.z >= 4096f) {
            if (viewPos.z / 256f > bp.visibilityDistance) return -1
            xx4 = 31
        } else {
            xx4 = (viewPos.z / 128f).toInt().coerceAtLeast(0)
        }

        // Posição do observador (origem) no referencial do modelo: Rᵀ·(0 − pos).
        tmp.set(pos).scl(-1f)
        camModel.set(tmp.dot(ship.sidev), tmp.dot(ship.roofv), tmp.dot(ship.nosev))

        if (faceVisible.size < bp.faceCount) faceVisible = BooleanArray(bp.faceCount)
        for (f in 0 until bp.faceCount) {
            val i = f * 3
            val d = bp.faceNormals[i] * (camModel.x - bp.facePoints[i]) +
                bp.faceNormals[i + 1] * (camModel.y - bp.facePoints[i + 1]) +
                bp.faceNormals[i + 2] * (camModel.z - bp.facePoints[i + 2])
            faceVisible[f] = d > 0f || bp.faceAlwaysVisible[f] || xx4 > bp.faceVisibility[f]
        }

        if (world.size < bp.vertexCount * 3) world = FloatArray(bp.vertexCount * 3)
        for (v in 0 until bp.vertexCount) {
            ship.toWorld(bp.vertices[v * 3], bp.vertices[v * 3 + 1], bp.vertices[v * 3 + 2], tmp)
            view.toView(tmp.x, tmp.y, tmp.z, tmp)
            world[v * 3] = tmp.x; world[v * 3 + 1] = tmp.y; world[v * 3 + 2] = tmp.z
        }

        var count = 0
        for (e in 0 until bp.edgeCount) {
            if (bp.edgeVisibility[e] < xx4) continue
            val f1 = bp.edges[e * 4 + 2]; val f2 = bp.edges[e * 4 + 3]
            if (!faceVisible[f1] && !faceVisible[f2]) continue
            if (emit(bp.edges[e * 4], bp.edges[e * 4 + 1], sink)) count++
        }
        return count
    }

    private fun emit(a: Int, b: Int, sink: SegmentSink): Boolean {
        var x0 = world[a * 3]; var y0 = world[a * 3 + 1]; var z0 = world[a * 3 + 2]
        var x1 = world[b * 3]; var y1 = world[b * 3 + 1]; var z1 = world[b * 3 + 2]
        if (z0 < nearZ && z1 < nearZ) return false
        // Recorte no plano próximo z = nearZ.
        if (z0 < nearZ) {
            val t = (nearZ - z0) / (z1 - z0)
            x0 += (x1 - x0) * t; y0 += (y1 - y0) * t; z0 = nearZ
        } else if (z1 < nearZ) {
            val t = (nearZ - z1) / (z0 - z1)
            x1 += (x0 - x1) * t; y1 += (y0 - y1) * t; z1 = nearZ
        }
        val cx = viewportWidth * 0.5f
        val cy = viewportHeight * 0.5f
        val f = focal
        sink.segment(cx + x0 / z0 * f, cy - y0 / z0 * f, cx + x1 / z1 * f, cy - y1 / z1 * f)
        return true
    }

    /**
     * Projeta um ponto do referencial do jogador na vista atual: [out] recebe (x, y) de tela e z da vista.
     * Retorna false se estiver atrás do plano próximo.
     */
    fun projectWorld(p: Vec3, out: Vec3): Boolean {
        view.toView(p.x, p.y, p.z, tmp)
        return projectPoint(tmp.x, tmp.y, tmp.z, out)
    }

    /** Projeta um ponto já no referencial da vista. Retorna false se estiver atrás do plano próximo. */
    fun projectPoint(x: Float, y: Float, z: Float, out: Vec3): Boolean {
        if (z < nearZ) return false
        out.set(viewportWidth * 0.5f + x / z * focal, viewportHeight * 0.5f - y / z * focal, z)
        return true
    }
}
