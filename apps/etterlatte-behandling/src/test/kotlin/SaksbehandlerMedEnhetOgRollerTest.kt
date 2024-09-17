package no.nav.etterlatte

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.etterlatte.common.Enhet
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

class SaksbehandlerMedEnhetOgRollerTest {
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
                    listOf(SaksbehandlerEnhet(Enhet.PORSGRUNN.enhetNr, Enhet.PORSGRUNN.name)),
                    listOf(Enhet.PORSGRUNN.enhetNr),
                    listOf(
                        Enhet.AALESUND.enhetNr,
                        Enhet.STEINKJER.enhetNr,
                        Enhet.AALESUND_UTLAND.enhetNr,
                        Enhet.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Vanlig saksbehandler med utland",
                    listOf(
                        SaksbehandlerEnhet(Enhet.AALESUND.enhetNr, Enhet.AALESUND.name),
                        SaksbehandlerEnhet(Enhet.AALESUND_UTLAND.enhetNr, Enhet.AALESUND_UTLAND.name),
                    ),
                    listOf(Enhet.AALESUND.enhetNr, Enhet.AALESUND_UTLAND.enhetNr),
                    listOf(
                        Enhet.PORSGRUNN.enhetNr,
                        Enhet.STEINKJER.enhetNr,
                        Enhet.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Kontaktsenter",
                    listOf(SaksbehandlerEnhet(Enhet.OEST_VIKEN.enhetNr, Enhet.OEST_VIKEN.navn)),
                    emptyList<String>(),
                    listOf(
                        Enhet.AALESUND.enhetNr,
                        Enhet.STEINKJER.enhetNr,
                        Enhet.PORSGRUNN.enhetNr,
                        Enhet.AALESUND_UTLAND.enhetNr,
                        Enhet.UTLAND.enhetNr,
                    ),
                    false,
                ),
                Arguments.of(
                    "Ukjent",
                    listOf(SaksbehandlerEnhet("12345", "En annen enhet")),
                    emptyList<String>(),
                    emptyList<String>(),
                    false,
                ),
                Arguments.of(
                    "Vanlig saksbehandler med andre enheter enn bare de etterlatte kjenner til",
                    listOf(
                        SaksbehandlerEnhet(Enhet.PORSGRUNN.enhetNr, Enhet.PORSGRUNN.name),
                        SaksbehandlerEnhet("12345", "En annen enhet"),
                    ),
                    listOf(Enhet.PORSGRUNN.enhetNr),
                    listOf(
                        Enhet.AALESUND.enhetNr,
                        Enhet.STEINKJER.enhetNr,
                        Enhet.AALESUND_UTLAND.enhetNr,
                        Enhet.UTLAND.enhetNr,
                    ),
                    true,
                ),
                Arguments.of(
                    "Kontaktsenter med andre enheter enn bare de etterlatte kjenner til",
                    listOf(
                        SaksbehandlerEnhet(Enhet.OEST_VIKEN.enhetNr, Enhet.OEST_VIKEN.navn),
                        SaksbehandlerEnhet("12345", "En annen enhet"),
                    ),
                    emptyList<String>(),
                    listOf(
                        Enhet.AALESUND.enhetNr,
                        Enhet.STEINKJER.enhetNr,
                        Enhet.PORSGRUNN.enhetNr,
                        Enhet.AALESUND_UTLAND.enhetNr,
                        Enhet.UTLAND.enhetNr,
                    ),
                    false,
                ),
            )
    }
}
