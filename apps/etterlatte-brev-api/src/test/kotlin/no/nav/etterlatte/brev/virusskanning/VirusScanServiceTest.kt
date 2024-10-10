package no.nav.etterlatte.brev.virusskanning

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

internal class VirusScanServiceTest {
    val clamAv = mockk<ClamAvClient>()

    @BeforeEach
    internal fun `Set up`() {
        clearAllMocks()
    }

    @Test
    fun `Should return true if result contains FOUND`() {
        coEvery { clamAv.skann(any()) } returns
            listOf(
                ScanResult("eicar.com.txt", Status.FOUND),
            )

        val request =
            VirusScanRequest("Testdokument.pdf", getFileContent("src/test/resources/virusskanning/Testdokument.pdf"))

        runBlocking {
            val vedleggContainsVirus = VirusScanService(clamAv).filHarVirus(request)
            assertEquals(true, vedleggContainsVirus)
        }
    }

    @Test
    fun `Should return false if result only contains OK`() {
        coEvery { clamAv.skann(any()) } returns
            listOf(
                ScanResult("normalFile", Status.OK),
            )
        val request =
            VirusScanRequest("Testdokument.pdf", getFileContent("src/test/resources/virusskanning/Testdokument.pdf"))

        runBlocking {
            val vedleggContainsVirus = VirusScanService(clamAv).filHarVirus(request)
            assertEquals(false, vedleggContainsVirus)
        }
    }

    @Test
    fun `Should return true if result contains ERROR`() {
        coEvery { clamAv.skann(any()) } returns
            listOf(
                ScanResult("strangeFile", Status.ERROR),
            )

        val request =
            VirusScanRequest("Testdokument.pdf", getFileContent("src/test/resources/virusskanning/Testdokument.pdf"))

        runBlocking {
            val vedleggContainsVirus = VirusScanService(clamAv).filHarVirus(request)
            assertEquals(true, vedleggContainsVirus)
        }
    }

    private fun getFileContent(filepath: String): ByteArray = Files.readAllBytes(Paths.get(filepath))
}
