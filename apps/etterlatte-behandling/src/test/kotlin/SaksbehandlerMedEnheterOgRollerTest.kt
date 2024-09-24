package no.nav.etterlatte

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
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
        forventetSkriveEnheter: List<String>,
        forventetLeseEnheter: List<String>,
        tilgangTilOppgavebenken: Boolean,
    ) {
        val saksbehandlerService = mockk<SaksbehandlerService>()
        val identifiedBy = mockk<TokenValidationContext>()
        mockkStatic(TokenValidationContext::hentTokenClaimsForIssuerName)
        val tokenClaims = mockk<JwtTokenClaims>()
        val saksbehandlerMedRoller = mockk<SaksbehandlerMedRoller>()
        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        every {
            tokenClaims.getStringClaim(Claims.NAVident.name)
        } returns "NAVIdent"

        every {
            identifiedBy.hentTokenClaimsForIssuerName(any())
        } returns tokenClaims

        every {
            saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any())
        } returns enheterForSaksbehandler

        val saksbehandler = SaksbehandlerMedEnheterOgRoller(identifiedBy, saksbehandlerService, saksbehandlerMedRoller, brukerTokenInfo)
        val skriveEnheter = saksbehandler.enheterMedSkrivetilgang()
        val leseEnheter = saksbehandler.enheterMedLesetilgang(enheterForSaksbehandler.map { it.enhetsNummer }.toSet())

        skriveEnheter shouldContainExactlyInAnyOrder forventetSkriveEnheter
        leseEnheter shouldContainExactlyInAnyOrder forventetLeseEnheter

        saksbehandler.kanSeOppgaveBenken() shouldBe tilgangTilOppgavebenken
    }

    companion object {
        @JvmStatic
        fun saksbehandlere() =
            listOf(
                Arguments.of(
                    "Vanlig saksbehandler",
                    listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.name)),
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    listOf(
                        Enheter.AALESUND.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.AALESUND_UTLAND.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Vanlig saksbehandler med utland",
                    listOf(
                        SaksbehandlerEnhet(Enheter.AALESUND.enhetNr, Enheter.AALESUND.name),
                        SaksbehandlerEnhet(Enheter.AALESUND_UTLAND.enhetNr, Enheter.AALESUND_UTLAND.name),
                    ),
                    listOf(Enheter.AALESUND.enhetNr, Enheter.AALESUND_UTLAND.enhetNr),
                    listOf(
                        Enheter.PORSGRUNN.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Kontaktsenter",
                    listOf(SaksbehandlerEnhet(Enheter.OEST_VIKEN.enhetNr, Enheter.OEST_VIKEN.navn)),
                    emptyList<String>(),
                    listOf(
                        Enheter.AALESUND.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.PORSGRUNN.enhetNr,
                        Enheter.AALESUND_UTLAND.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                    false,
                ),
                Arguments.of(
                    "Ukjent",
                    listOf(SaksbehandlerEnhet(Enhetsnummer("9876"), "En annen enhet")),
                    emptyList<String>(),
                    emptyList<String>(),
                    false,
                ),
                Arguments.of(
                    "Vanlig saksbehandler med andre enheter enn bare de etterlatte kjenner til",
                    listOf(
                        SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.name),
                        SaksbehandlerEnhet(Enhetsnummer("9876"), "En annen enhet"),
                    ),
                    listOf(Enheter.PORSGRUNN.enhetNr),
                    listOf(
                        Enheter.AALESUND.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.AALESUND_UTLAND.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Kontaktsenter med andre enheter enn bare de etterlatte kjenner til",
                    listOf(
                        SaksbehandlerEnhet(Enheter.OEST_VIKEN.enhetNr, Enheter.OEST_VIKEN.navn),
                        SaksbehandlerEnhet(Enhetsnummer("9876"), "En annen enhet"),
                    ),
                    emptyList<String>(),
                    listOf(
                        Enheter.AALESUND.enhetNr,
                        Enheter.STEINKJER.enhetNr,
                        Enheter.PORSGRUNN.enhetNr,
                        Enheter.AALESUND_UTLAND.enhetNr,
                        Enheter.UTLAND.enhetNr,
                    ),
                    false,
                ),
            )
    }
}
