package no.nav.etterlatte.behandling.klage

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlageServiceTest {
    private lateinit var service: KlageService

    @Test
    fun `hent klage test`() {
        val klagedaoMock = mockk<KlageDao>()
        val klageid = UUID.randomUUID()
        every { klagedaoMock.hentKlage(klageid) } returns null
        service = KlageServiceImpl(klagedaoMock, mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val hentKlage = service.hentKlage(klageid)
        assertNull(hentKlage)
    }
}
