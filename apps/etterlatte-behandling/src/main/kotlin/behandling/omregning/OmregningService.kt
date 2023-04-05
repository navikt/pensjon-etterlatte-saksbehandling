package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.time.LocalDate
import java.util.*

class OmregningService(
    private val revurderingFactory: RevurderingFactory,
    private val behandlingService: GenerellBehandlingService
) {
    fun opprettOmregning(
        sakId: Long,
        fradato: LocalDate,
        prosesstype: Prosesstype
    ): Pair<UUID, UUID> {
        val forrigeBehandling = behandlingService.hentSenestIverksatteBehandling(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")
        val behandlingId = inTransaction {
            when (prosesstype) {
                Prosesstype.AUTOMATISK -> revurderingFactory.opprettAutomatiskRevurdering(
                    sakId = sakId,
                    forrigeBehandling = forrigeBehandling,
                    fradato = fradato,
                    revurderingAarsak = RevurderingAarsak.REGULERING
                )
                Prosesstype.MANUELL -> revurderingFactory.opprettManuellRevurdering(
                    sakId = sakId,
                    forrigeBehandling = forrigeBehandling,
                    revurderingAarsak = RevurderingAarsak.REGULERING
                )
            }
        }.lagretBehandling.id

        return Pair(behandlingId, forrigeBehandling.id)
    }
}