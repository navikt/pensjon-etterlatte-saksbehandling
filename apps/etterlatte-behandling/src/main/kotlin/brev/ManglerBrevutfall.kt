package brev

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.util.UUID

class ManglerBrevutfall(
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_BREVUTFALL",
        detail = "Behandling mangler brevutfall, som er påkrevd. Legg til dette ved å lagre Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )
