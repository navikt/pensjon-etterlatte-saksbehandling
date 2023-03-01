package no.nav.etterlatte.behandling.omberegning

import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.regulering.ReguleringFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import java.time.LocalDate
import java.util.*

class OmberegningService(
    private val reguleringFactory: ReguleringFactory,
    private val behandlingService: GenerellBehandlingService
) {
    fun opprettOmberegning(
        sakId: Long,
        fradato: LocalDate,
        prosesstype: Prosesstype
    ): UUID {
        val forrigeBehandling = behandlingService.hentBehandlingerISak(sakId)
            .maxByOrNull { it.behandlingOpprettet }
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")
        return inTransaction {
            reguleringFactory.opprettRegulering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                fradato = fradato,
                prosesstype = prosesstype
            )
        }.lagretBehandling.id
    }
}