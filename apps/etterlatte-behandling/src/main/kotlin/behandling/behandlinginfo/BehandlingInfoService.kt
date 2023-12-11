package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallException
import no.nav.etterlatte.libs.common.behandling.EtterbetalingException
import no.nav.etterlatte.libs.common.behandling.EtterbetalingNy
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import java.util.UUID

class BehandlingInfoService(
    private val behandlingInfoDao: BehandlingInfoDao,
    private val behandlingService: BehandlingService,
) {
    fun lagreBrevutfall(brevutfall: Brevutfall): Brevutfall {
        val behandling =
            behandlingService.hentBehandling(brevutfall.behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)
        sjekkEtterbetalingFoerVirkningstidspunkt(behandling, brevutfall)
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevutfall)

        return behandlingInfoDao.lagre(brevutfall)
    }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? {
        return behandlingInfoDao.hent(behandlingId)
    }

    fun hentEtterbetaling(behandlingId: UUID): EtterbetalingNy? {
        return behandlingInfoDao.hent(behandlingId)?.etterbetalingNy
    }

    private fun sjekkBehandlingKanEndres(behandling: Behandling) {
        if (!behandling.status.kanEndres()) throw BrevutfallException.BehandlingKanIkkeEndres(behandling.id, behandling.status)
    }

    private fun sjekkEtterbetalingFoerVirkningstidspunkt(
        behandling: Behandling,
        brevutfall: Brevutfall,
    ) {
        val virkningstidspunkt =
            behandling.virkningstidspunkt?.dato
                ?: throw BrevutfallException.VirkningstidspunktIkkeSatt(behandling.id)

        if (brevutfall.etterbetalingNy != null && brevutfall.etterbetalingNy!!.fom < virkningstidspunkt) {
            throw EtterbetalingException.EtterbetalingFraDatoErFoerVirk(brevutfall.etterbetalingNy!!.fom, virkningstidspunkt)
        }
    }

    private fun sjekkAldersgruppeSattVedBarnepensjon(
        behandling: Behandling,
        brevutfall: Brevutfall,
    ) {
        if (behandling.sak.sakType == SakType.BARNEPENSJON && brevutfall.aldersgruppe == null) {
            throw BrevutfallException.AldergruppeIkkeSatt()
        }
    }
}
