package no.nav.etterlatte.libs.ktor.token

import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `brevbatch har en egen systemidentitet uten innsendertoken`() {
        val bruker = HardkodaSystembruker.brevBatch
        assertEquals("brev-batch", bruker.ident())
        assertEquals(APP, bruker.getClaims()?.getClaimAsString(Claims.idtyp))
        assertEquals(null, bruker.getClaims()?.getClaimAsString(Claims.azp_name))
    }
}
