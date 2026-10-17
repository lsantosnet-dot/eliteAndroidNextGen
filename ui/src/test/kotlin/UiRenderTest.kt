import com.mercador.ui.Preview
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO

class UiRenderTest {
    @Test
    fun `as telas renderizam sem janela e nao saem em branco`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mercador-ui-test").also { it.deleteRecursively() }
        val files = Preview.renderAll(dir)
        assertEquals(listOf("01-mapa.png", "02-estacao.png", "05-perseguicao.png", "03-confronto.png", "04-livro.png", "06-historia.png", "07-relatorio.png", "08-dossie.png", "09-bar.png", "10-jogos.png"), files.map { it.name })
        for (f in files) {
            val img = ImageIO.read(f)
            assertEquals(720, img.width); assertEquals(1480, img.height)
            // variedade de cores: uma tela vazia ou de cor única reprova
            val colors = HashSet<Int>()
            for (y in 0 until img.height step 7) for (x in 0 until img.width step 7) colors.add(img.getRGB(x, y))
            assertTrue(colors.size > 40, "${f.name}: só ${colors.size} cores")
            // fundo noir: o canto inferior esquerdo é escuro
            val c = img.getRGB(2, 1470)
            val lum = ((c shr 16 and 255) + (c shr 8 and 255) + (c and 255)) / 3
            assertTrue(lum < 60, "${f.name}: fundo claro demais ($lum)")
        }
    }
}
