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
import java.util.Base64

internal class VirusScanServiceTest {
    private val clamAvClientMock = mockk<ClamAvClient>()
    private val loggingMeta = LoggingMeta("1")

    @BeforeEach
    internal fun `Set up`() {
        clearAllMocks()
    }

    @Test
    fun `Should return true if result contains FOUND`() {
        coEvery { clamAvClientMock.virusScanVedlegg(any()) } returns
            listOf(
                ScanResult("normalFile", Status.OK),
                ScanResult("eicar.com.txt", Status.FOUND),
            )
        val contentImage = base64Encode(getFileContent("src/test/resources/virusskanning/doctor.jpeg"))
        val contentText = base64Encode(getFileContent("src/test/resources/virusskanning/random.txt"))

        val vedleggBilde =
            Vedlegg(Content("Base64Container", contentImage), "image/jpeg", "Bilde av lege")
        val vedleggText =
            Vedlegg(Content("Base64Container", contentText), "text/plain", "eicar.com")

        runBlocking {
            val vedleggContainsVirus =
                VirusScanService(clamAvClientMock)
                    .vedleggContainsVirus(listOf(vedleggBilde, vedleggText), loggingMeta)
            assertEquals(true, vedleggContainsVirus)
        }
    }

    @Test
    fun `Should return false if result only contains OK`() {
        coEvery { clamAvClientMock.virusScanVedlegg(any()) } returns
            listOf(
                ScanResult("normalFile", Status.OK),
                ScanResult("anotherNormalFile", Status.OK),
            )
        val contentImage = base64Encode(getFileContent("src/test/resources/virusskanning/doctor.jpeg"))

        val vedleggImage1 =
            Vedlegg(Content("Base64Container", contentImage), "image/jpeg", "Et bilde fra lege")
        val vedleggImage2 =
            Vedlegg(Content("Base64Container", contentImage), "image/jpeg", "Et til bilde fra lege")

        runBlocking {
            val vedleggContainsVirus =
                VirusScanService(clamAvClientMock)
                    .vedleggContainsVirus(listOf(vedleggImage1, vedleggImage2), loggingMeta)
            assertEquals(false, vedleggContainsVirus)
        }
    }

    @Test
    fun `Should return true if result contains ERROR`() {
        coEvery { clamAvClientMock.virusScanVedlegg(any()) } returns
            listOf(
                ScanResult("normalFile", Status.OK),
                ScanResult("strangeFile", Status.ERROR),
            )

        val contentImage = base64Encode(getFileContent("src/test/resources/virusskanning/doctor.jpeg"))
        val vedleggImage1 =
            Vedlegg(Content("Base64Container", contentImage), "image/jpeg", "Bilde av lege")
        val vedleggImage2 =
            Vedlegg(Content("Base64Container", contentImage), "image/jpeg", "Samme lege")

        runBlocking {
            val vedleggContainsVirus =
                VirusScanService(clamAvClientMock)
                    .vedleggContainsVirus(listOf(vedleggImage1, vedleggImage2), loggingMeta)
            assertEquals(true, vedleggContainsVirus)
        }
    }

    @Test
    fun `Should return false when file size is lower than 300 megabytes`() {
        val base64EncodedContent = base64Encode(getFileContent("src/test/resources/virusskanning/random.txt"))
        val vedlegg =
            Vedlegg(Content("Base64Container", base64EncodedContent), "image/jpeg", "image_of_file")
        val file = Base64.getMimeDecoder().decode(vedlegg.content.content)
        assertEquals(false, fileSizeLagerThan300MegaBytes(file, loggingMeta))
    }

    private fun getFileContent(filepath: String): ByteArray = Files.readAllBytes(Paths.get(filepath))

    private fun base64Encode(byteArray: ByteArray): String = Base64.getMimeEncoder().encodeToString(byteArray)
}
