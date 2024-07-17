package no.nav.etterlatte.token

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BrukerTokenInfoTest {
    fun genererClaimSetSystembruker() =
        JwtTokenClaims(
            JWTClaimsSet
                .Builder()
                .claim(Claims.idtyp.name, "app")
                .claim(Claims.azp_name.name, "cluster:appname:dev")
                .build(),
        )

    @Test
    fun `er maskin-til-maskin hvis idtype=app`() {
        assertTrue(BrukerTokenInfo.of("a", "saksbehandler", genererClaimSetSystembruker(), idtyp = "app") is Systembruker)
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
