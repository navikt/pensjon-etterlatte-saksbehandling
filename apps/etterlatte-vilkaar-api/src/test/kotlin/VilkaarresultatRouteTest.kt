import io.ktor.auth.LolSecMediator
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.VilkaarService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.model.VilkaarResultatForBehandling
import no.nav.etterlatte.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*


class VilkaarresultatRouteTest {

    private val vilkaarService = mockk<VilkaarService>()
    private val securityContextMediator = spyk<LolSecMediator>()

    @Test
    fun `skal returnere vilkaarresultat`() {
        val behandlingId = UUID.randomUUID().toString()

        coEvery { vilkaarService.hentVilkaarResultat(behandlingId) } returns vilkaarResultatForBehandling(behandlingId)

        withTestApplication({ module(securityContextMediator, vilkaarService) }) {
            handleRequest(HttpMethod.Get, VILKAARRESULTAT_ENDEPUNKT.withParameter(behandlingId)) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())

                val vilkaarResultatForBehandling =
                    objectMapper.readValue(response.content!!, VilkaarResultatForBehandling::class.java)
                assertEquals(behandlingId, vilkaarResultatForBehandling.behandling.toString())

                verify { vilkaarService.hentVilkaarResultat(behandlingId) }
                verify { securityContextMediator.secureRoute(any(), any()) }
                confirmVerified(vilkaarService)
            }
        }
    }

    companion object {
        const val VILKAARRESULTAT_ENDEPUNKT = "/vilkaarresultat"
    }

    private fun String.withParameter(parameter: String) = "$this/$parameter"

    private fun vilkaarResultatForBehandling(behandlingId: String) = VilkaarResultatForBehandling(
        behandling = UUID.fromString(behandlingId),
        avdoedSoeknad = null,
        soekerSoeknad = null,
        soekerPdl = null,
        avdoedPdl = null,
        gjenlevendePdl = null,
        versjon = 1,
        VilkaarResultat(
            VurderingsResultat.OPPFYLT,
            emptyList(),
            LocalDateTime.now()
        )
    )
}