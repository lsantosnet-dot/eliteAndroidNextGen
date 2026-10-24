package com.mercador.core.flight


/**
 * Modelo 3D de uma nave no formato do Elite original (blocos VERTEX / EDGE / FACE do
 * disassembly de Mark Moxon).
 *
 * Coordenadas no referencial da própria nave: x = lateral (sidev), y = teto (roofv),
 * z = nariz (nosev). Os valores são os inteiros originais do BBC Elite.
 *
 * Remoção de linhas ocultas como no original: uma aresta é desenhada se pelo menos
 * uma das duas faces que ela separa estiver voltada para o observador. A "visibilidade"
 * de arestas é a distância máxima (em unidades de z_hi) em que o detalhe ainda é
 * desenhado — 31 = sempre.
 *
 * As normais das faces vêm do bloco FACE do original quando ele é fornecido (modelos da
 * versão em disco): como no LL9 parte 5, a face é visível se (normal / 2^[normalScale] −
 * observador) · normal < 0 — a normal reduzida faz as vezes de um ponto da face, perto do
 * centro da nave —, e faces sem normal (como a 15 das placas de liga) estão sempre
 * visíveis. Sem o bloco FACE, as normais são calculadas a partir da geometria (os valores
 * do original são aproximações inteiras dessas mesmas normais).
 */
class ShipBlueprint private constructor(
    val name: String,
    /** x, y, z intercalados. */
    val vertices: FloatArray,
    /** v1, v2, face1, face2 por aresta. */
    val edges: IntArray,
    val edgeVisibility: IntArray,
    /** Arestas de detalhe (fendas, decalques, canhão): não definem o plano das faces. */
    private val edgeDetail: BooleanArray,
    val faceCount: Int,
    /** Byte #13 do modelo: além de z_hi (z/256) maior que isto, a nave vira um ponto. */
    val visibilityDistance: Int,
    /** Bytes #1-2 ("targetable area"): o laser acerta se x² + y² < este valor (HITCH). */
    val targetableArea: Int,
    /** Atributos do bloco do modelo: energia (#14), velocidade (#15), recompensa (#10-11, em décimos de Cr). */
    val maxEnergy: Int,
    val maxSpeed: Int,
    val bounty: Int,
    /** Byte #19: potência do laser nos bits 3-7 e mísseis nos bits 0-2. */
    val laserByte: Int,
    /** Byte #0: máximo de contêineres de carga soltos ao ser destruída. */
    val maxCanisters: Int,
    /** Byte #6/4: vértice de onde sai o laser. */
    val gunVertex: Int,
    /** (Byte #7 − 6)/4: quantos vértices geram nuvens na explosão. */
    val explosionVertices: Int,
    /** Byte #0, bits 4-7 (versão em disco): mercadoria − 1 ao ser recolhida (0 = nenhuma). */
    private val cargoItem: Int,
    /** Normais do bloco FACE (x, y, z, visibilidade por face) e a escala delas (byte #18); vazio = calcular. */
    private val sourceFaces: IntArray,
    private val normalScale: Int,
) {
    /** Mercadoria (0..16) obtida ao recolher este objeto com os coletores, ou −1. */
    val scoopItem: Int get() = if (cargoItem > 0) cargoItem + 1 else -1

    /** Faces sem normal no original: sempre visíveis (LL9). */
    val faceAlwaysVisible: BooleanArray = BooleanArray(faceCount)

    /**
     * LL9 parte 5: com a distância XX4 (0..31) maior que este valor, a face é desenhada sem
     * testar a normal (31 = sempre testa).
     */
    val faceVisibility: IntArray = IntArray(faceCount) { 31 }

    val laserPower: Int get() = laserByte ushr 3
    val missiles: Int get() = laserByte and 7

    val vertexCount: Int get() = vertices.size / 3
    val edgeCount: Int get() = edges.size / 4

    /** Normais unitárias (x, y, z) por face, apontando para fora. */
    val faceNormals: FloatArray = FloatArray(faceCount * 3)

    /** Um ponto sobre cada face (centroide dos vértices da face). */
    val facePoints: FloatArray = FloatArray(faceCount * 3)

    /** Raio da esfera envolvente (para descartar naves fora de vista). */
    val boundingRadius: Float

    init {
        var r2 = 0f
        for (i in 0 until vertexCount) {
            val x = vertices[i * 3]; val y = vertices[i * 3 + 1]; val z = vertices[i * 3 + 2]
            r2 = maxOf(r2, x * x + y * y + z * z)
        }
        boundingRadius = kotlin.math.sqrt(r2)
        if (sourceFaces.isEmpty()) computeFaces() else useSourceFaces()
    }

    fun vertex(i: Int, out: Vec3): Vec3 = out.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2])

    /** Índices dos vértices que pertencem à face [f] (via arestas de contorno, não de detalhe). */
    fun faceVertices(f: Int): Set<Int> {
        val set = linkedSetOf<Int>()
        for (e in 0 until edgeCount) {
            val f1 = edges[e * 4 + 2]; val f2 = edges[e * 4 + 3]
            if (!edgeDetail[e] && f1 != f2 && (f1 == f || f2 == f)) {
                set += edges[e * 4]; set += edges[e * 4 + 1]
            }
        }
        return set
    }

    private fun useSourceFaces() {
        val n = Vec3()
        val scale = 1f / (1 shl normalScale) // LL92/LL93: a normal é dividida por 2^X
        for (f in 0 until faceCount) {
            val i = f * 3
            val src = f * 4
            if (src + 3 >= sourceFaces.size || (sourceFaces[src] == 0 && sourceFaces[src + 1] == 0 && sourceFaces[src + 2] == 0)) {
                faceAlwaysVisible[f] = true
                continue
            }
            faceVisibility[f] = sourceFaces[src + 3]
            n.set(sourceFaces[src].toFloat(), sourceFaces[src + 1].toFloat(), sourceFaces[src + 2].toFloat())
            facePoints[i] = n.x * scale; facePoints[i + 1] = n.y * scale; facePoints[i + 2] = n.z * scale
            n.nor()
            faceNormals[i] = n.x; faceNormals[i + 1] = n.y; faceNormals[i + 2] = n.z
        }
    }

    private fun computeFaces() {
        val center = Vec3()
        for (i in 0 until vertexCount) center.add(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2])
        center.scl(1f / vertexCount)

        val a = Vec3(); val b = Vec3(); val c = Vec3(); val n = Vec3(); val tmp = Vec3()
        for (f in 0 until faceCount) {
            val vs = faceVertices(f).toList()
            require(vs.size >= 3) { "$name: face $f tem só ${vs.size} vértices" }

            // Centroide da face.
            val centroid = Vec3()
            vs.forEach { centroid.add(vertex(it, tmp)) }
            centroid.scl(1f / vs.size)

            // Normal pelo triângulo de maior área entre os vértices da face (robusto a quase-colinearidade).
            var best = 0f
            for (i in vs.indices) for (j in i + 1 until vs.size) for (k in j + 1 until vs.size) {
                vertex(vs[i], a); vertex(vs[j], b); vertex(vs[k], c)
                b.sub(a); c.sub(a)
                tmp.set(b).crs(c)
                val len2 = tmp.len2()
                if (len2 > best) { best = len2; n.set(tmp) }
            }
            n.nor()
            // Orienta para fora do centro do casco.
            tmp.set(centroid).sub(center)
            if (n.dot(tmp) < 0f) n.scl(-1f)

            faceNormals[f * 3] = n.x; faceNormals[f * 3 + 1] = n.y; faceNormals[f * 3 + 2] = n.z
            facePoints[f * 3] = centroid.x; facePoints[f * 3 + 1] = centroid.y; facePoints[f * 3 + 2] = centroid.z
        }
    }

    /** DSL que espelha a sintaxe dos blocos VERTEX/EDGE/FACE do disassembly. */
    class Builder(private val name: String) {
        private val verts = ArrayList<Float>()
        private val edgeData = ArrayList<Int>()
        private val edgeVis = ArrayList<Int>()
        private val edgeDet = ArrayList<Boolean>()
        private var faces = 0

        /** Byte #13 do bloco do modelo ("Visibility distance"). */
        var visibilityDistance = 31

        /** Bytes #1-2 do bloco do modelo ("Targetable area"). */
        var targetableArea = 0

        private var energy = 0; private var speed = 0; private var laserByte = 0
        private var bounty = 0; private var canisters = 0; private var gunVertex = 0; private var explosion = 4
        private var cargoItem = 0
        private val faceData = ArrayList<Int>()

        /** Byte #18 do bloco do modelo: as normais do bloco FACE estão escaladas por 2^n. */
        var normalScale = 0

        fun stats(
            energy: Int, speed: Int, laserByte: Int, bounty: Int, canisters: Int, gunVertex: Int,
            explosionVertices: Int, cargoItem: Int = 0,
        ) {
            this.energy = energy; this.speed = speed; this.laserByte = laserByte; this.bounty = bounty
            this.canisters = canisters; this.gunVertex = gunVertex; this.explosion = explosionVertices
            this.cargoItem = cargoItem
        }

        /** FACE: normal de uma face e sua distância de visibilidade, na ordem do original. */
        fun face(nx: Int, ny: Int, nz: Int, visibility: Int = 31) {
            faceData += nx; faceData += ny; faceData += nz; faceData += visibility
        }

        fun vertex(x: Int, y: Int, z: Int) {
            verts += x.toFloat(); verts += y.toFloat(); verts += z.toFloat()
        }

        /**
         * [detail] marca arestas que só "pegam carona" na visibilidade das faces (como o canhão,
         * que sai do casco). Arestas com face1 == face2 são sempre detalhe.
         */
        fun edge(v1: Int, v2: Int, face1: Int, face2: Int, visibility: Int, detail: Boolean = false) {
            edgeData += v1; edgeData += v2; edgeData += face1; edgeData += face2
            edgeVis += visibility
            edgeDet += detail || face1 == face2
            faces = maxOf(faces, face1 + 1, face2 + 1)
        }

        fun build(): ShipBlueprint =
            ShipBlueprint(
                name, verts.toFloatArray(), edgeData.toIntArray(), edgeVis.toIntArray(),
                edgeDet.toBooleanArray(), maxOf(faces, faceData.size / 4), visibilityDistance, targetableArea,
                energy, speed, bounty, laserByte, canisters, gunVertex, explosion,
                cargoItem, faceData.toIntArray(), normalScale,
            )
    }

    companion object {
        fun define(name: String, block: Builder.() -> Unit): ShipBlueprint = Builder(name).apply(block).build()
    }
}
