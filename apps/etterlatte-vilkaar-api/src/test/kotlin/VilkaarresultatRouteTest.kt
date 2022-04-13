import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VilkaarService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.model.VurdertVilkaar
import no.nav.etterlatte.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sikkerhet.tokenTestSupportAcceptsAllTokens
import java.time.LocalDateTime
import java.util.*


class VilkaarresultatRouteTest {

    private val vilkaarService = mockk<VilkaarService>()

    @Test
    @Disabled
    fun `skal returnere vilkaarresultat`() {
        val behandlingId = UUID.randomUUID().toString()

        //coEvery { vilkaarService.hentVilkaarResultat(behandlingId) } returns vilkaarResultatForBehandling(behandlingId)

        withTestApplication({
            module(
                Authentication.Configuration::tokenTestSupportAcceptsAllTokens,
                vilkaarService
            )
        }) {
            handleRequest(HttpMethod.Get, VILKAARRESULTAT_ENDEPUNKT.withParameter(behandlingId)) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())

                val vurdertVilkaar =
                    objectMapper.readValue(response.content!!, VurdertVilkaar::class.java)
                assertEquals(behandlingId, vurdertVilkaar.behandling.toString())

                verify { vilkaarService.hentVilkaarResultat(behandlingId) }
                confirmVerified(vilkaarService)
            }
        }
    }

    companion object {
        const val VILKAARRESULTAT_ENDEPUNKT = "/vurdertvilkaar"
    }

    private fun String.withParameter(parameter: String) = "$this/$parameter"

    private fun vilkaarResultatForBehandling(behandlingId: String) = VurdertVilkaar(
        behandling = UUID.fromString(behandlingId),
        avdoedSoeknad = null,
        soekerSoeknad = null,
        soekerPdl = null,
        avdoedPdl = null,
        gjenlevendePdl = null,
        versjon = 1,
        objectMapper.valueToTree(
            VilkaarResultat(
                VurderingsResultat.OPPFYLT,
                emptyList(),
                LocalDateTime.now()
            )
        )
    )
}