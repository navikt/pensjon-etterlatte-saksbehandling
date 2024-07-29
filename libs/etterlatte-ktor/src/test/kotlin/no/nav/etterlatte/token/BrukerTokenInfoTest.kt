package no.nav.etterlatte.libs.ktor.token

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BrukerTokenInfoTest {
    private fun genererClaimSetSystembruker() =
        tokenMedClaims(
            mapOf(
                Claims.idtyp to APP,
                Claims.azp_name to "cluster:appname:dev",
            ),
        )

    @Test
    fun `er maskin-til-maskin hvis idtype=app`() {
        assertTrue(BrukerTokenInfo.of("a", "saksbehandler", genererClaimSetSystembruker(), idtyp = APP) is Systembruker)
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
