package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate
import java.util.*

class OmregningService(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService
) {
    suspend fun opprettOmregning(
        sakId: Long,
        fraDato: LocalDate,
        prosessType: Prosesstype
    ): Triple<UUID, UUID, SakType> {
        val forrigeBehandling = behandlingService.hentSisteIverksatte(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

        val persongalleri = grunnlagService.hentPersongalleri(sakId)

        val behandling = when (prosessType) {
            Prosesstype.AUTOMATISK -> revurderingService.opprettAutomatiskRevurdering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                virkningstidspunkt = fraDato,
                kilde = Vedtaksloesning.GJENNY,
                persongalleri = persongalleri,
                merknad = null
            )

            Prosesstype.MANUELL -> throw Exception("Støtter ikke prosesstype MANUELL")
        } ?: throw Exception("Opprettelse av revurdering feilet for $sakId")
        return Triple(behandling.id, forrigeBehandling.id, behandling.sak.sakType)
    }
}