package no.nav.etterlatte.oppgave

import java.util.UUID

data class EndrePaaVentRequest(
    val aarsak: PaaVentAarsak? = null,
    val merknad: String,
    val paavent: Boolean,
) {
    init {
        if (paavent) {
            require(aarsak != null) { "Må ha valgt årsak hvis man setter paavent = true" }
        }
    }

    fun toDomain(oppgaveId: UUID) = PaaVent(oppgaveId, aarsak, merknad, paavent)
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
)
