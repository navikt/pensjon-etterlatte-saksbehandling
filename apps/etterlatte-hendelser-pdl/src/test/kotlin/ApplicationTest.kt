package no.nav.etterlatte

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.hendelserpdl.LyttPaaHendelser
import no.nav.etterlatte.hendelserpdl.LyttPaaHendelserProvider
import no.nav.etterlatte.hendelserpdl.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest {
    private val stream = mockk<LyttPaaHendelser>()

    @Test
    fun testRoot() {
        every { stream.getAntallIterasjoner() } returns 22
        every { stream.getAntallDoedsMeldinger() } returns 22
        every { stream.getAntallMeldinger() } returns 22

        LyttPaaHendelserProvider.setStream(stream)
        testApplication {
            application { this.module(LyttPaaHendelserProvider) }
            assertEquals(HttpStatusCode.OK, client.get("/status").status)
        }
    }
}