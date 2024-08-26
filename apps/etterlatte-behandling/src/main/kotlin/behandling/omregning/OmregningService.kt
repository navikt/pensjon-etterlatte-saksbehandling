package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagServiceImpl
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingOgOppfoelging
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.util.UUID

class OmregningService(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagServiceImpl,
    private val revurderingService: AutomatiskRevurderingService,
    private val omregningDao: OmregningDao,
) {
    fun hentForrigeBehandling(sakId: no.nav.etterlatte.libs.common.sak.SakId) =
        behandlingService.hentSisteIverksatte(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

    fun hentPersongalleri(id: UUID) = runBlocking { grunnlagService.hentPersongalleri(id) }

    fun opprettOmregning(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        fraDato: LocalDate,
        revurderingAarsak: Revurderingaarsak,
        prosessType: Prosesstype,
        forrigeBehandling: Behandling,
        persongalleri: Persongalleri,
        oppgavefrist: Tidspunkt?,
    ): RevurderingOgOppfoelging {
        if (prosessType == Prosesstype.MANUELL) {
            throw StoetterIkkeProsesstypeManuell()
        }

        revurderingService.validerSakensTilstand(sakId, revurderingAarsak)

        return requireNotNull(
            revurderingService.opprettAutomatiskRevurdering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                revurderingAarsak = revurderingAarsak,
                virkningstidspunkt = fraDato,
                kilde = Vedtaksloesning.GJENNY,
                persongalleri = persongalleri,
                frist = oppgavefrist,
            ),
        ) { "Opprettelse av revurdering feilet for $sakId" }
    }

    fun oppdaterKjoering(
        request: KjoeringRequest,
        bruker: BrukerTokenInfo,
    ) {
        if (request.status == KjoeringStatus.FEILA) {
            behandlingService.hentAapenRegulering(request.sakId)?.let {
                behandlingService.avbrytBehandling(it, bruker)
            }
        }
        omregningDao.oppdaterKjoering(request)
    }

    fun kjoeringFullfoert(request: LagreKjoeringRequest) {
        if (request.status != KjoeringStatus.FERDIGSTILT) {
            throw IllegalStateException("Prøver å lagre at kjøring er fullført, men status er ikke ferdigstilt.")
        }
        omregningDao.lagreKjoering(request)
    }
}

class StoetterIkkeProsesstypeManuell :
    UgyldigForespoerselException(
        code = "StoetterIkkeProsesstypeManuell",
        detail = "Støtter ikke omregning for manuell behandling",
    )
