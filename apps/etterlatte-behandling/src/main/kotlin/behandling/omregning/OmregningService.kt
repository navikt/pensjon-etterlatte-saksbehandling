package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingOgOppfoelging
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.LocalDate
import java.util.UUID

class OmregningService(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: AutomatiskRevurderingService,
) {
    fun hentForrigeBehandling(sakId: Long) =
        behandlingService.hentSisteIverksatte(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

    fun hentPersongalleri(id: UUID) = runBlocking { grunnlagService.hentPersongalleri(id) }

    fun opprettOmregning(
        sakId: Long,
        fraDato: LocalDate,
        prosessType: Prosesstype,
        forrigeBehandling: Behandling,
        persongalleri: Persongalleri,
    ): RevurderingOgOppfoelging {
        if (prosessType == Prosesstype.MANUELL) {
            throw StoetterIkkeProsesstypeManuell()
        }
        return requireNotNull(
            revurderingService.opprettAutomatiskRevurdering(
                sakId = sakId,
                forrigeBehandling = forrigeBehandling,
                revurderingAarsak = Revurderingaarsak.REGULERING,
                virkningstidspunkt = fraDato,
                kilde = Vedtaksloesning.GJENNY,
                persongalleri = persongalleri,
            ),
        ) { "Opprettelse av revurdering feilet for $sakId" }
    }
}

class StoetterIkkeProsesstypeManuell : UgyldigForespoerselException(
    code = "StoetterIkkeProsesstypeManuell",
    detail = "Støtter ikke omregning for manuell behandling",
)
