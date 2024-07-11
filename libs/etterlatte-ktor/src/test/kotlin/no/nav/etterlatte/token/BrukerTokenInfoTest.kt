package no.nav.etterlatte.token

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BrukerTokenInfoTest {
    @Test
    fun `er maskin-til-maskin hvis idtype=app`() {
        assertTrue(BrukerTokenInfo.of("a", "saksbehandler", null, idtyp = "app") is Systembruker)
    }

    @Test
    fun `er ikke maskin-til-maskin hvis idtype != app`() {
        assertFalse(BrukerTokenInfo.of("a", "saksbehandler", null, idtyp = "ikkeapp") is Systembruker)
    }

    @Test
    fun `er ikke maskin-til-maskin hvis idtype er tomt`() {
        assertFalse(BrukerTokenInfo.of("a", "saksbehandler", null, idtyp = null) is Systembruker)
    }
}
