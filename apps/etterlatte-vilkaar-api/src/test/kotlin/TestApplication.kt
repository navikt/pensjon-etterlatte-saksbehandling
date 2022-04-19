package no.nav.etterlatte

import io.ktor.auth.Authentication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import java.util.*


fun main() {
    val applicationContext = spyk(ApplicationContext("/application-test.conf", System.getenv().toMutableMap())) {
        every { tokenValidering() } returns Authentication.Configuration::tokenTestSupportAcceptsAllTokens
        every { vilkaarDao() } returns mockk {
            every { hentVilkaarResultat(any()) } returns vilkaarResultatForBehandling(UUID.randomUUID().toString())
        }
    }

    Server(applicationContext).run()
}