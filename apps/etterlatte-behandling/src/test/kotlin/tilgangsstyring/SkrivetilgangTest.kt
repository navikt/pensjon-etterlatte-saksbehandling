package tilgangsstyring

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.Self
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.azureAdAttestantGjennyClaim
import no.nav.etterlatte.azureAdLesetilgangGjennyClaim
import no.nav.etterlatte.azureAdSaksbehandlerGjennyClaim
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.ManglerSkrivetilgangAarsak
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.tilgangsstyring.Skrivetilgang
import no.nav.etterlatte.tilgangsstyring.harSkrivetilgangTilEnhet
import no.nav.etterlatte.tilgangsstyring.vurderSkrivetilgang
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SkrivetilgangTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("saksbehandlere")
    fun `vurderSkrivetilgang gir riktig aarsak for saksbehandler`(
        beskrivelse: String,
        adGrupper: List<String>,
        saksbehandlersEnheter: List<Enhetsnummer>,
        sakensEnhet: Enhetsnummer,
        forventet: Skrivetilgang,
    ) {
        val saksbehandler = saksbehandler(adGrupper, saksbehandlersEnheter)

        val skrivetilgang = vurderSkrivetilgang(saksbehandler, sakensEnhet)

        skrivetilgang shouldBe forventet
        // Avgjørelsen skal være den samme som sjekkSkrivetilgang bruker for å håndheve tilgangen
        skrivetilgang.harSkrivetilgang shouldBe saksbehandler.harSkrivetilgangTilEnhet(sakensEnhet)
    }

    @Test
    fun `systembruker har skrivetilgang, som i sjekkSkrivetilgang`() {
        vurderSkrivetilgang(mockk<SystemUser>(), Enheter.PORSGRUNN.enhetNr) shouldBe Skrivetilgang.HAR_SKRIVETILGANG
    }

    @Test
    fun `annen bruker enn saksbehandler og systembruker har ikke skrivetilgang`() {
        vurderSkrivetilgang(Self("jobb"), Enheter.PORSGRUNN.enhetNr) shouldBe
            Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.UKJENT_BRUKER)
    }

    private fun saksbehandler(
        adGrupper: List<String>,
        enheter: List<Enhetsnummer>,
    ): SaksbehandlerMedEnheterOgRoller {
        val saksbehandlerService = mockk<SaksbehandlerService>()
        val identifiedBy = mockk<TokenValidationContext>()
        mockkStatic(TokenValidationContext::hentTokenClaimsForIssuerName)
        val tokenClaims = mockk<JwtTokenClaims>()

        every { tokenClaims.getStringClaim(Claims.NAVident.name) } returns "NAVIdent"
        every { identifiedBy.hentTokenClaimsForIssuerName(any()) } returns tokenClaims
        every { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            enheter.map { SaksbehandlerEnhet(it, it.enhetNr) }

        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                saksbehandler = simpleSaksbehandler(ident = "NAVIdent", claims = mapOf(Claims.groups to adGrupper)),
                saksbehandlerGroupIdsByKey =
                    mapOf(
                        AzureGroup.SAKSBEHANDLER_GJENNY to azureAdSaksbehandlerGjennyClaim,
                        AzureGroup.ATTESTANT_GJENNY to azureAdAttestantGjennyClaim,
                        AzureGroup.GJENNY_LES to azureAdLesetilgangGjennyClaim,
                    ),
            )

        return SaksbehandlerMedEnheterOgRoller(
            identifiedBy,
            saksbehandlerService,
            saksbehandlerMedRoller,
            mockk<BrukerTokenInfo>(),
        )
    }

    companion object {
        private val PORSGRUNN = Enheter.PORSGRUNN.enhetNr
        private val STEINKJER = Enheter.STEINKJER.enhetNr

        @JvmStatic
        fun saksbehandlere() =
            listOf(
                Arguments.of(
                    "Saksbehandler i sakens enhet har skrivetilgang",
                    listOf(azureAdSaksbehandlerGjennyClaim),
                    listOf(PORSGRUNN),
                    PORSGRUNN,
                    Skrivetilgang.HAR_SKRIVETILGANG,
                ),
                Arguments.of(
                    "Attestant i sakens enhet har skrivetilgang",
                    listOf(azureAdAttestantGjennyClaim),
                    listOf(PORSGRUNN),
                    PORSGRUNN,
                    Skrivetilgang.HAR_SKRIVETILGANG,
                ),
                Arguments.of(
                    "Saksbehandler i annen enhet mangler skrivetilgang til sakens enhet",
                    listOf(azureAdSaksbehandlerGjennyClaim),
                    listOf(STEINKJER),
                    PORSGRUNN,
                    Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG),
                ),
                Arguments.of(
                    "Bare lesetilgang i Gjenny gir ikke skrivetilgang, selv i sakens enhet",
                    listOf(azureAdLesetilgangGjennyClaim),
                    listOf(PORSGRUNN),
                    PORSGRUNN,
                    Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.MANGLER_ROLLE),
                ),
                Arguments.of(
                    "Uten roller mangler rolle, selv i sakens enhet",
                    emptyList<String>(),
                    listOf(PORSGRUNN),
                    PORSGRUNN,
                    Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.MANGLER_ROLLE),
                ),
                Arguments.of(
                    "Uten roller og i annen enhet er manglende rolle aarsaken",
                    emptyList<String>(),
                    listOf(STEINKJER),
                    PORSGRUNN,
                    Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.MANGLER_ROLLE),
                ),
            )
    }
}
