package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import java.time.LocalDate
import java.util.UUID

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val sakService: SakService,
    val vedtakKlient: VedtakKlient,
    val behandlingService: BehandlingService,
    val beregningKlient: BeregningKlient,
) {
    fun hentAlleAktiveEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> = dao.hentAlleAktiveEtteroppgjoerForSak(sakId)

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

    suspend fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ) {
        logger.info(
            "Forsøker å opprette etteroppgjør for sakId=$sakId og inntektsaar=$inntektsaar",
        )
        if (dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) != null) {
            throw IkkeTillattException(
                "ETTEROPPGJOER_EKSISTERER_ALLEREDE",
                "Kan ikke opprette etteroppgjør for sak=$sakId og inntektsaar=$inntektsaar. Etteroppgjør er allerede opprettet",
            )
        }

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatteBehandling(sakId)
                ?: throw InternfeilException("Kunne ikke hente siste iverksatte behandling for sakId=$sakId")

        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                harSanksjon = utledSanksjoner(sisteIverksatteBehandling.id, inntektsaar),
                harInstitusjonsopphold = utledInstitusjonsopphold(sisteIverksatteBehandling.id),
                harOpphoer = sisteIverksatteBehandling.opphoerFraOgMed !== null,
                erBosattUtland = sisteIverksatteBehandling.erBosattUtland(),
                harAdressebeskyttelse =
                    sisteIverksatteBehandling.sak.adressebeskyttelse?.harAdressebeskyttelse() == true ||
                        sisteIverksatteBehandling.sak.erSkjermet == true,
                harAktivitetskrav = utledAktivitetskrav(sisteIverksatteBehandling.id, sisteIverksatteBehandling.sak.sakType, inntektsaar),
            )

        dao.lagreEtteroppgjoer(etteroppgjoer)
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

        return sanksjoner.any { sanksjon ->
            sanksjon.fom.year == inntektsaar && sanksjon.tom == null
        }
    }

    private suspend fun utledInstitusjonsopphold(behandlingId: UUID): Boolean {
        val beregningOgAvkorting =
            beregningKlient.hentBeregningOgAvkorting(
                behandlingId,
                HardkodaSystembruker.etteroppgjoer,
            )
        return beregningOgAvkorting.perioder.any { it.institusjonsopphold != null }
    }
}
