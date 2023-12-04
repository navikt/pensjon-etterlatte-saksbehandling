package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.util.UUID

class BrevoppsettService(
    private val brevoppsettDao: BrevoppsettDao,
    private val behandlingService: BehandlingService,
) {
    fun lagreBrevoppsett(brevoppsett: Brevoppsett): Brevoppsett {
        val behandling =
            behandlingService.hentBehandling(brevoppsett.behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)
        sjekkEtterbetalingEtterVirkningstidspunkt(behandling, brevoppsett)

        return brevoppsettDao.lagre(brevoppsett)
    }

    fun hentBrevoppsett(behandlingId: UUID): Brevoppsett? {
        return brevoppsettDao.hent(behandlingId)
    }

    private fun sjekkBehandlingKanEndres(behandling: Behandling) {
        if (!behandling.status.kanEndres()) throw BehandlingKanIkkeEndresException(behandling)
    }

    private fun sjekkEtterbetalingEtterVirkningstidspunkt(
        behandling: Behandling,
        brevoppsett: Brevoppsett,
    ) {
        val virkningstidspunkt =
            behandling.virkningstidspunkt?.dato
                ?: throw VirkningstidspunktIkkeSattException(behandling)

        if (brevoppsett.etterbetaling != null && brevoppsett.etterbetaling.fom > virkningstidspunkt) {
            throw EtterbetalingException.FraDatoErFoerVirk(brevoppsett.etterbetaling.fom, virkningstidspunkt)
        }
    }
}

class BehandlingKanIkkeEndresException(behandling: Behandling) : IkkeTillattException(
    code = "KAN_IKKE_ENDRES",
    detail = "Behandling ${behandling.id} har status ${behandling.status} og kan ikke endres.",
)

class VirkningstidspunktIkkeSattException(behandling: Behandling) : UgyldigForespoerselException(
    code = "VIRKNINGSTIDSPUNKT_IKKE_SATT",
    detail = "Behandling ${behandling.id} har ikke satt virkningstidspunkt.",
)
