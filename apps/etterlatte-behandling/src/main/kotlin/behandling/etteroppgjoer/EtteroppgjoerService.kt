package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import java.time.LocalDate
import java.util.UUID

enum class EtteroppgjoerSvarfrist(
    val value: String,
) {
    ETT_MINUTT("1 minute"),
    EN_MND("1 month"),
}

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val sakService: SakService,
    val vedtakKlient: VedtakKlient,
    val behandlingService: BehandlingService,
    val beregningKlient: BeregningKlient,
) {
    fun hentAlleAktiveEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> = dao.hentAlleAktiveEtteroppgjoerForSak(sakId)

    fun hentEtteroppgjoerMedSvarfristUtloept(
        inntektsaar: Int,
        svarfrist: EtteroppgjoerSvarfrist,
    ): List<Etteroppgjoer>? = dao.hentEtteroppgjoerMedSvarfristUtloept(inntektsaar, svarfrist)

    fun hentEtteroppgjoerForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)

    fun hentEtteroppgjoerForStatus(
        status: EtteroppgjoerStatus,
        inntektsaar: Int,
    ): List<Etteroppgjoer> = dao.hentEtteroppgjoerForStatus(status, inntektsaar)

    fun hentEtteroppgjoerForFilter(
        filter: EtteroppgjoerFilter,
        inntektsaar: Int,
    ): List<Etteroppgjoer> = dao.hentEtteroppgjoerForFilter(filter, inntektsaar)

    fun oppdaterEtteroppgjoerStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.oppdaterEtteroppgjoerStatus(sakId, inntektsaar, status)
    }

    fun oppdaterEtteroppgjoerFerdigstiltForbehandling(forbehandling: EtteroppgjoerForbehandling) {
        oppdaterEtteroppgjoerStatus(forbehandling.sak.id, forbehandling.aar, EtteroppgjoerStatus.FERDIGSTILT_FORBEHANDLING)
        oppdaterSisteFerdigstiltForbehandlingId(forbehandling.sak.id, forbehandling.aar, forbehandling.id)
    }

    fun oppdaterSisteFerdigstiltForbehandlingId(
        sakId: SakId,
        inntektsaar: Int,
        forbehandlingId: UUID,
    ) {
        dao.oppdaterFerdigstiltForbehandlingId(sakId, inntektsaar, forbehandlingId)
    }

    suspend fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? {
        logger.info(
            "Forsøker å opprette etteroppgjør for sakId=$sakId og inntektsaar=$inntektsaar",
        )

        val etteroppgjoerForSak = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)
        if (etteroppgjoerForSak != null && etteroppgjoerForSak.status != EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER) {
            logger.info(
                "Etteroppgjoer for sakId=$sakId og inntektsaar=$inntektsaar er allerede opprettet med status ${etteroppgjoerForSak.status}.",
            )
            return null
        }

        val sisteIverksatteVedtak =
            vedtakKlient
                .hentIverksatteVedtak(sakId, brukerTokenInfo = HardkodaSystembruker.etteroppgjoer)
                .sortedByDescending { it.datoAttestert }
                .firstOrNull { it.vedtakType != VedtakType.OPPHOER }
                ?: throw InternfeilException("Fant ikke siste iverksatte vedtak i sak=$sakId")
        val sisteIverksatteBehandling =
            krevIkkeNull(behandlingService.hentBehandling(sisteIverksatteVedtak.behandlingId)) {
                "Siste iverksatte vedtak (id=${sisteIverksatteVedtak.id} peker på en behandling " +
                    "med id=${sisteIverksatteVedtak.behandlingId} som ikke finnes"
            }

        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                harSanksjon = utledSanksjoner(sisteIverksatteBehandling.id, inntektsaar),
                harInstitusjonsopphold = utledInstitusjonsopphold(sisteIverksatteBehandling.id, inntektsaar),
                harOpphoer = sisteIverksatteBehandling.opphoerFraOgMed !== null,
                harBosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type !== UtlandstilknytningType.NASJONAL,
                harAdressebeskyttelseEllerSkjermet =
                    sisteIverksatteBehandling.sak.adressebeskyttelse?.harAdressebeskyttelse() == true ||
                        sisteIverksatteBehandling.sak.erSkjermet == true,
                harAktivitetskrav =
                    utledAktivitetskrav(
                        sisteIverksatteBehandling.id,
                        sisteIverksatteBehandling.sak.sakType,
                        inntektsaar,
                    ),
            )

        dao.lagreEtteroppgjoer(etteroppgjoer)
        return etteroppgjoer
    }

    private fun utledAktivitetskrav(
        behandlingId: UUID,
        sakType: SakType,
        inntektsaar: Int,
    ): Boolean {
        val aktivitetsKravDato = LocalDate.of(inntektsaar, 7, 1)
        val sisteDoedsdato = behandlingService.hentFoersteDoedsdato(behandlingId, sakType) ?: return false
        return sisteDoedsdato.isBefore(aktivitetsKravDato)
    }

    private suspend fun utledSanksjoner(
        behandlingId: UUID,
        inntektsaar: Int,
    ): Boolean {
        val sanksjoner =
            beregningKlient.hentSanksjoner(
                behandlingId,
                HardkodaSystembruker.etteroppgjoer,
            )

        return sanksjoner?.any { sanksjon ->
            sanksjon.fom.year <= inntektsaar && (sanksjon.tom?.year ?: inntektsaar) >= inntektsaar
        } == true
    }

    private suspend fun utledInstitusjonsopphold(
        behandlingId: UUID,
        inntektsaar: Int,
    ): Boolean {
        val beregningsGrunnlag =
            beregningKlient.hentBeregningsgrunnlag(behandlingId, HardkodaSystembruker.etteroppgjoer)
        return beregningsGrunnlag.institusjonsopphold.any {
            it.fom.year <= inntektsaar && (it.tom?.year ?: inntektsaar) >= inntektsaar
        }
    }
}
