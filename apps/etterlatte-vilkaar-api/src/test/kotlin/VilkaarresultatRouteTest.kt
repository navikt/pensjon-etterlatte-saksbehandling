package no.nav.etterlatte

import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.VurdertVilkaar
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*


class VilkaarresultatRouteTest {

    private val vilkaarService = mockk<VilkaarService>()
    private val applicationContext = mockk<ApplicationContext>()

    @Test
    fun `skal returnere vilkaarresultat`() {
        val behandlingId = UUID.randomUUID().toString()

        coEvery { vilkaarService.hentVilkaarResultat(behandlingId) } returns vilkaarResultatForBehandling(behandlingId)
        every { applicationContext.vilkaarService(any()) } returns vilkaarService
        every { applicationContext.vilkaarDao() } returns mockk()
        every { applicationContext.tokenValidering() } returns Authentication.Configuration::tokenTestSupportAcceptsAllTokens

        withTestApplication({
            module(applicationContext)
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

}