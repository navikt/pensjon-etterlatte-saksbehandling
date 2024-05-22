package no.nav.etterlatte.oppgave

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class EndrePaaVentRequest(
    val aarsak: PaaVentAarsak? = null,
    val merknad: String,
    val paaVent: Boolean,
) {
    fun toDomain(oppgaveId: UUID) = PaaVent(oppgaveId, aarsak, merknad, paaVent)
}

enum class PaaVentAarsak {
    OPPLYSNING_FRA_BRUKER,
    OPPLYSNING_FRA_ANDRE,
    KRAVGRUNNLAG_SPERRET,
    ANNET,
}

data class PaaVent(
    val oppgaveId: UUID,
    val aarsak: PaaVentAarsak? = null,
    val merknad: String,
    val paavent: Boolean,
) {
    init {
        if (paavent) {
            requireUgyldigForespoerselException(
                aarsak != null,
                "AARSAK_KAN_IKKE_VAERE_NULL",
                "Årsak må velges om en behandling skal settes på vent",
            )
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun requireUgyldigForespoerselException(
    value: Boolean,
    code: String,
    msg: String,
) {
    contract {
        returns() implies value
    }
    if (!value) {
        throw UgyldigForespoerselException(code = code, detail = msg)
    }
}
