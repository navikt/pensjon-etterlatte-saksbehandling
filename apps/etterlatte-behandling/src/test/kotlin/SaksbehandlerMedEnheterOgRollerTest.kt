package no.nav.etterlatte

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
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
    ) {
        val saksbehandlerService = mockk<SaksbehandlerService>()
        val identifiedBy = mockk<TokenValidationContext>()
        mockkStatic(TokenValidationContext::hentTokenClaimsForIssuerName)
        val tokenClaims = mockk<JwtTokenClaims>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleSaksbehandler() } returns true
                every { harRolleAttestant() } returns false
            }
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
        val skriveEnheter = saksbehandler.hentEnheterMedSkrivetilgang()
        val leseEnheter = saksbehandler.enheterMedLesetilgang(enheterForSaksbehandler.map { it.enhetsNummer }.toSet())

        skriveEnheter shouldContainExactlyInAnyOrder forventetSkriveEnheter
        leseEnheter shouldContainExactlyInAnyOrder forventetLeseEnheter
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("oppgavebenkRoller")
    fun `kanSeOppgaveBenken avgjoeres av rolle, ikke enhet`(
        beskrivelse: String,
        harRolleLesetilgang: Boolean,
        harRolleSaksbehandler: Boolean,
        harRolleAttestant: Boolean,
        forventetTilgangTilOppgavebenken: Boolean,
    ) {
        val saksbehandlerService = mockk<SaksbehandlerService>()
        val identifiedBy = mockk<TokenValidationContext>()
        mockkStatic(TokenValidationContext::hentTokenClaimsForIssuerName)
        val tokenClaims = mockk<JwtTokenClaims>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleLesetilgang() } returns harRolleLesetilgang
                every { harRolleSaksbehandler() } returns harRolleSaksbehandler
                every { harRolleAttestant() } returns harRolleAttestant
            }
        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        every { tokenClaims.getStringClaim(Claims.NAVident.name) } returns "NAVIdent"
        every { identifiedBy.hentTokenClaimsForIssuerName(any()) } returns tokenClaims
        // Enhetene er uten betydning for kanSeOppgaveBenken() – kun rolle skal avgjøre dette.
        every { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns emptyList()

        val saksbehandler = SaksbehandlerMedEnheterOgRoller(identifiedBy, saksbehandlerService, saksbehandlerMedRoller, brukerTokenInfo)

        saksbehandler.kanSeOppgaveBenken() shouldBe forventetTilgangTilOppgavebenken
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roller")
    fun `skrivetilgang krever saksbehandler- eller attestantrolle i tillegg til saksbehandlende enhet`(
        beskrivelse: String,
        adGrupper: List<String>,
        forventetSkriveEnheter: List<Enhetsnummer>,
    ) {
        val saksbehandlerService = mockk<SaksbehandlerService>()
        val identifiedBy = mockk<TokenValidationContext>()
        mockkStatic(TokenValidationContext::hentTokenClaimsForIssuerName)
        val tokenClaims = mockk<JwtTokenClaims>()
        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        every { tokenClaims.getStringClaim(Claims.NAVident.name) } returns "NAVIdent"
        every { identifiedBy.hentTokenClaimsForIssuerName(any()) } returns tokenClaims
        every {
            saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any())
        } returns listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.name))

        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                saksbehandler = simpleSaksbehandler(ident = "NAVIdent", claims = mapOf(Claims.groups to adGrupper)),
                saksbehandlerGroupIdsByKey =
                    mapOf(
                        AzureGroup.SAKSBEHANDLER to azureAdSaksbehandlerClaim,
                        AzureGroup.SAKSBEHANDLER_GJENNY to azureAdSaksbehandlerGjennyClaim,
                        AzureGroup.ATTESTANT to azureAdAttestantClaim,
                        AzureGroup.ATTESTANT_GJENNY to azureAdAttestantGjennyClaim,
                    ),
            )

        val saksbehandler =
            SaksbehandlerMedEnheterOgRoller(identifiedBy, saksbehandlerService, saksbehandlerMedRoller, brukerTokenInfo)

        saksbehandler.hentEnheterMedSkrivetilgang() shouldContainExactlyInAnyOrder forventetSkriveEnheter
    }

    companion object {
        @JvmStatic
        fun roller() =
            listOf(
                Arguments.of(
                    "Kun lesetilgang i Gjenny gir ingen skrivetilgang",
                    listOf(azureAdLesetilgangGjennyClaim),
                    emptyList<Enhetsnummer>(),
                ),
                Arguments.of(
                    "Ingen roller gir ingen skrivetilgang",
                    emptyList<String>(),
                    emptyList<Enhetsnummer>(),
                ),
                Arguments.of(
                    "Saksbehandler i Gjenny gir skrivetilgang",
                    listOf(azureAdSaksbehandlerGjennyClaim),
                    listOf(Enheter.PORSGRUNN.enhetNr),
                ),
                Arguments.of(
                    "Saksbehandler i Pesys gir skrivetilgang",
                    listOf(azureAdSaksbehandlerClaim),
                    listOf(Enheter.PORSGRUNN.enhetNr),
                ),
                Arguments.of(
                    "Attestant i Gjenny gir skrivetilgang",
                    listOf(azureAdAttestantGjennyClaim),
                    listOf(Enheter.PORSGRUNN.enhetNr),
                ),
            )

        @JvmStatic
        fun oppgavebenkRoller() =
            listOf(
                Arguments.of("Lesetilgang (GJENNY_LES) gir tilgang til oppgavebenken", true, false, false, true),
                Arguments.of("Saksbehandlerrolle gir tilgang til oppgavebenken", false, true, false, true),
                Arguments.of("Attestantrolle gir tilgang til oppgavebenken", false, false, true, true),
                Arguments.of("Ingen av rollene gir ikke tilgang til oppgavebenken", false, false, false, false),
            )

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
                ),
                Arguments.of(
                    "Ukjent",
                    listOf(SaksbehandlerEnhet(Enhetsnummer("9876"), "En annen enhet")),
                    emptyList<String>(),
                    emptyList<String>(),
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
                ),
            )
    }
}
