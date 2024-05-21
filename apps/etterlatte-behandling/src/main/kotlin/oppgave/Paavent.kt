package no.nav.etterlatte.oppgave

import java.util.UUID

data class EndrePaaVentRequest(
    val aarsak: PaaventAarsak? = null,
    val merknad: String,
    val paavent: Boolean,
) {
    fun toDomain(oppgaveId: UUID) = Paavent(oppgaveId, aarsak, merknad, paavent)
}

enum class PaaventAarsak {
    OPPLYSNING_FRA_BRUKER,
    OPPLYSNING_FRA_ANDRE,
    KRAVGRUNNLAG_SPERRET,
    ANNET,
}

data class Paavent(
    val oppgaveId: UUID,
    val aarsak: PaaventAarsak? = null,
    val merknad: String,
    val paavent: Boolean,
)
