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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

class OmregningService(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagServiceImpl,
    private val revurderingService: AutomatiskRevurderingService,
) {
    fun hentForrigeBehandling(sakId: Long) =
        behandlingService.hentSisteIverksatte(sakId)
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

    fun hentPersongalleri(id: UUID) = runBlocking { grunnlagService.hentPersongalleri(id) }

    fun opprettOmregning(
        sakId: Long,
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
}

class StoetterIkkeProsesstypeManuell : UgyldigForespoerselException(
    code = "StoetterIkkeProsesstypeManuell",
    detail = "Støtter ikke omregning for manuell behandling",
)
