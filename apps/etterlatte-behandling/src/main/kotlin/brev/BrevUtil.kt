package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sikkerLogg

fun hentVergeForSak(
    sakType: SakType,
    brevutfallDto: BrevutfallDto?,
    grunnlag: Grunnlag,
): Verge? {
    val soekerPdl =
        grunnlag.soeker.hentSoekerPdlV1()
            ?: throw InternfeilException(
                "Finner ikke søker i grunnlaget. Dette kan komme av flere ting, bl.a. endret ident på bruker. " +
                    "Hvis dette ikke er tilfellet må feilen meldes i Porten.",
            )

    val verger =
        hentVerger(
            soekerPdl.verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
            grunnlag.soeker.hentFoedselsnummer()?.verdi,
        )
    return if (verger.size == 1) {
        val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident
        if (vergeFnr == null) {
            logger.error(
                "Vi genererer et brev til en person som har verge uten ident. Det er verdt å følge " +
                    "opp saken ekstra, for å sikre at det ikke blir noe feil her (koble på fag). saken har " +
                    "id=${grunnlag.metadata.sakId}. Denne loggmeldingen kan nok fjernes etter at løpet her" +
                    " er kvalitetssikret.",
            )
            UkjentVergemaal()
        } else {
            // TODO: Hente navn direkte fra Grunnlag eller PDL
            val vergenavn = "placeholder for vergenavn"

            Vergemaal(
                vergenavn,
                vergeFnr,
            )
        }
    } else if (verger.size > 1) {
        logger.info(
            "Fant flere verger for bruker med fnr ${grunnlag.soeker.hentFoedselsnummer()?.verdi} i " +
                "mapping av verge til brev.",
        )
        sikkerLogg.info(
            "Fant flere verger for bruker med fnr ${
                grunnlag.soeker.hentFoedselsnummer()?.verdi?.value
            } i mapping av verge til brev.",
        )
        UkjentVergemaal()
    } else if (sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(brevutfallDto?.aldersgruppe)) {
        grunnlag.hentForelderVerge()
    } else {
        null
    }
}

data class SaksbehandlerOgAttestantBrev(
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
)

/**
 * Både attestant og saksbehandler til brev følger samme prioritert rekkefølge:
 *  1. bruk det som er lagret på vedtaket
 *  2. bruk det som er saksbehandler for behandlingsoppgaven
 *  3. bruk innlogget bruker (kun for saksbehandler)
 *
 *  Siden saksbehandler er obligatorisk mens attestant ikke er det sender vi ikke
 *  med en attestant med mindre vi er "sikre" på at det er en riktig ident for
 *  brevet.
 */
fun hentSaksbehandlerOgAttestantForVedtak(
    vedtakDto: VedtakDto,
    oppgaveForBehandling: OppgaveIntern?,
    brukerTokenInfo: BrukerTokenInfo,
): SaksbehandlerOgAttestantBrev {
    val saksbehandlerFraVedtak = vedtakDto.vedtakFattet?.ansvarligSaksbehandler
    val saksbehandlerFraOppgave = oppgaveForBehandling?.saksbehandler?.ident
    val saksbehandlerFraToken = brukerTokenInfo.ident()
    val saksbehandler =
        listOf(saksbehandlerFraVedtak, saksbehandlerFraOppgave, saksbehandlerFraToken).firstNotNullOf { it }

    val attestantFraVedtak = vedtakDto.attestasjon?.attestant
    val attestantFraOppgave = oppgaveForBehandling?.saksbehandler?.ident?.takeIf { oppgaveForBehandling.erAttestering() }
    val attestant = listOf(attestantFraVedtak, attestantFraOppgave).firstOrNull { it != null }

    return SaksbehandlerOgAttestantBrev(
        saksbehandlerIdent = saksbehandler,
        attestantIdent = attestant,
    )
}
