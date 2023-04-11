package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.time.LocalDate
import java.util.*

class OmregningService(
    private val behandlingService: GenerellBehandlingService,
    private val revurderingService: RevurderingService
) {
    fun opprettOmregning(
        sakId: Long,
        fraDato: LocalDate,
        prosessType: Prosesstype
    ): Pair<UUID?, UUID> {
        val forrigeBehandling = behandlingService.hentSenestIverksatteBehandling(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

        val behandling = when (prosessType) {
            Prosesstype.AUTOMATISK -> revurderingService.opprettAutomatiskRevurdering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                fraDato = fraDato,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.DOFFEN
            )

            Prosesstype.MANUELL -> revurderingService.opprettManuellRevurdering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.DOFFEN
            )
        }

        return Pair(behandling?.id, forrigeBehandling.id)
    }
}