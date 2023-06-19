package behandling

import java.util.*

internal class BehandlingStatusServiceTest {

    /*@Test TODO: skrive om
    fun `kan ikke attestere uten attestant-rolle`() {
        val service = BehandlingStatusServiceImpl(mockk(), mockk())

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", "123").build()
        val bruker = Saksbehandler("", "ident", JwtTokenClaims(jwtclaims))
        assertThrows<IllegalStateException> {
            service.sjekkOmKanAttestere(
                UUID.randomUUID(),
                SaksbehandlerMedRoller(bruker)
            )
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
            service.sjekkOmKanAttestere(
                UUID.randomUUID(),
                SaksbehandlerMedRoller(bruker)
            )
        }
    }*/
}