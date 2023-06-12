package behandling

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.config.AzureGroup
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class BehandlingStatusServiceTest {

    @Test
    fun `kan ikke attestere uten attestant-rolle`() {
        val service = BehandlingStatusServiceImpl(mockk(), mockk())

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", "123").build()
        val bruker = Saksbehandler("", "ident", JwtTokenClaims(jwtclaims))
        assertThrows<IllegalStateException> {
            service.sjekkOmKanAttestere(UUID.randomUUID(), bruker, mapOf(AzureGroup.ATTESTANT to "789"))
        }
    }

    @Test
    fun `kan attestere med attestant-rolle`() {
        val service = BehandlingStatusServiceImpl(
            mockk(),
            mockk<GenerellBehandlingService>().also {
                every { it.hentBehandling(any()) } returns foerstegangsbehandling(
                    sakId = 1L,
                    status = BehandlingStatus.FATTET_VEDTAK
                )
            }
        )

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", "123").build()
        val bruker = Saksbehandler("", "ident", JwtTokenClaims(jwtclaims))
        assertDoesNotThrow {
            service.sjekkOmKanAttestere(UUID.randomUUID(), bruker, mapOf(AzureGroup.ATTESTANT to "123"))
        }
    }
}