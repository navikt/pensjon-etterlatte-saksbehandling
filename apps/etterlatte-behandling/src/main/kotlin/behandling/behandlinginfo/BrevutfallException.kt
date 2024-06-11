package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.util.UUID

sealed class BrevutfallException {
    class BehandlingKanIkkeEndres(
        behandlingId: UUID,
        status: BehandlingStatus,
    ) : IkkeTillattException(
            code = "KAN_IKKE_ENDRES",
            detail = "Behandling $behandlingId har status $status og kan ikke endres.",
        )

    class VirkningstidspunktIkkeSatt(
        behandlingId: UUID,
    ) : UgyldigForespoerselException(
            code = "VIRKNINGSTIDSPUNKT_IKKE_SATT",
            detail = "Behandling $behandlingId har ikke satt virkningstidspunkt.",
        )

    class AldergruppeIkkeSatt :
        IkkeTillattException(
            code = "ALDERGRUPPE_IKKE_SATT",
            detail = "Aldersgruppe må være satt for behandling av barnepensjon.",
        )

    class LavEllerIngenInntektIkkeSatt :
        IkkeTillattException(
            code = "LAV_ELLER_INGEN_INNTEKT_IKKE_SATT",
            detail = "Lav eller ingen inntekt må være satt for behandling av omstillingsstønad.",
        )

    class FeilutbetalingIkkeSatt :
        IkkeTillattException(
            code = "FEILUTBETALING_IKKE_SATT",
            detail = "Feilutbetaling må være satt for behandling av omstillingsstønad ved revurderinger.",
        )
}
