package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import java.util.UUID

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val sakLesDao: SakLesDao,
    val sakService: SakService,
    val vedtakKlient: VedtakKlient,
    val behandlingService: BehandlingService,
    val beregningKlient: BeregningKlient,
) {
    // når vi mottar hendelse fra skatt, sjekk om ident skal ha etteroppgjør
    fun skalHaEtteroppgjoer(
        ident: String,
        inntektsaar: Int,
    ): SkalHaEtteroppgjoerResultat {
        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer = sak?.let { dao.hentEtteroppgjoerForInntektsaar(it.id, inntektsaar) }

        val skalHaEtteroppgjoer = etteroppgjoer?.skalHaEtteroppgjoer() ?: false

        return SkalHaEtteroppgjoerResultat(
            skalHaEtteroppgjoer,
            etteroppgjoer,
        )
    }

    fun hentAlleAktiveEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> = dao.hentAlleAktiveEtteroppgjoerForSak(sakId)

    fun hentEtteroppgjoerForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer? = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)

    fun hentEtteroppgjoerForStatus(
        status: EtteroppgjoerStatus,
        inntektsaar: Int,
    ): List<Etteroppgjoer> = dao.hentEtteroppgjoerForStatus(status, inntektsaar)

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
            logger.error("Kan ikke opprette etteroppgjør for sak=$sakId og inntektsaar=$inntektsaar. Etteroppgjør er allerede opprettet")
            return
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
                harInstitusjonsEllerFengselsopphold = utledInstitusjonsopphold(sisteIverksatteBehandling.id),
                harOpphoer = sisteIverksatteBehandling.opphoerFraOgMed === null,
                harBosattUtland = sisteIverksatteBehandling.erBosattUtland(),
            )

        dao.lagerEtteroppgjoer(etteroppgjoer)
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
            sanksjon.fom.year == inntektsaar || sanksjon.tom?.year == inntektsaar
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

data class SkalHaEtteroppgjoerResultat(
    val skalHaEtteroppgjoer: Boolean,
    val etteroppgjoer: Etteroppgjoer?,
)
