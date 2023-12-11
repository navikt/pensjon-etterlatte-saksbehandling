package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class Brevutfall(
    val behandlingId: UUID,
    val etterbetalingNy: EtterbetalingNy?,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde,
)

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}

sealed class BrevutfallException {
    class BehandlingKanIkkeEndres(behandlingId: UUID, status: BehandlingStatus) : IkkeTillattException(
        code = "KAN_IKKE_ENDRES",
        detail = "Behandling $behandlingId har status $status og kan ikke endres.",
    )

    class VirkningstidspunktIkkeSatt(behandlingId: UUID) : UgyldigForespoerselException(
        code = "VIRKNINGSTIDSPUNKT_IKKE_SATT",
        detail = "Behandling $behandlingId har ikke satt virkningstidspunkt.",
    )

    class AldergruppeIkkeSatt() : IkkeTillattException(
        code = "ALDERGRUPPE_IKKE_SATT",
        detail = "Aldersgruppe må være satt for behandling av barnepensjon.",
    )
}
