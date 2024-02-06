import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.ktor.hentTokenClaims
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SaksbehandlerMedEnheterOgRollerTest {
    @ParameterizedTest(
        name = "{0}",
    )
    @MethodSource("saksbehandlere")
    fun `saksbehandler faar riktig skrive og lesetilgang enheter`(
        beskrivelse: String,
        enheterForSaksbehandler: List<SaksbehandlerEnhet>,
        nasjonalTilgang: Boolean,
        forventetSkriveEnheter: List<String>,
        forventetLeseEnheter: List<String>,
    ) {
        val navAnsattKlient = mockk<NavAnsattKlient>()
        val identifiedBy = mockk<TokenValidationContext>()
        val tokenClaims = mockk<JwtTokenClaims>()
        val saksbehandlerMedRoller = mockk<SaksbehandlerMedRoller>()

        every {
            tokenClaims.getStringClaim(any())
        } returns "NAVIdent"

        every {
            identifiedBy.hentTokenClaims(any())
        } returns tokenClaims

        coEvery {
            navAnsattKlient.hentEnheterForSaksbehandler(any())
        } returns enheterForSaksbehandler

        every {
            saksbehandlerMedRoller.harRolleNasjonalTilgang()
        } returns nasjonalTilgang

        val saksbehandler = SaksbehandlerMedEnheterOgRoller(identifiedBy, navAnsattKlient, saksbehandlerMedRoller)

        val skriveEnheter = saksbehandler.enheterMedSkrivetilgang()
        val leseEnheter = saksbehandler.enheterMedLesetilgang()

        skriveEnheter shouldContainExactlyInAnyOrder forventetSkriveEnheter
        leseEnheter shouldContainExactlyInAnyOrder forventetLeseEnheter
    }

    companion object {
        @JvmStatic
        fun saksbehandlere() =
            listOf(
                Arguments.of(
                    "Vanlig saksbehandler",
                    listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.name)),
                    false,
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    emptyList<String>(),
                ),
                Arguments.of(
                    "Saksbehandler med nasjonal tilgang",
                    listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.name)),
                    true,
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr, Enheter.AALESUND_UTLAND.enhetNr, Enheter.UTLAND.enhetNr),
                ),
                Arguments.of(
                    "Kontaktsenter",
                    listOf(SaksbehandlerEnhet(Enheter.TROENDELAG.enhetNr, Enheter.TROENDELAG.navn)),
                    false,
                    emptyList<String>(),
                    listOf(
                        Enheter.AALESUND.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.PORSGRUNN.enhetNr,
                        Enheter.AALESUND_UTLAND.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                ),
            )
    }
}
