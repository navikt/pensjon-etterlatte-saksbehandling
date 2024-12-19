package no.nav.etterlatte.brev.model

object Etterbetaling {
    // TODO: kan forenkles
    fun fraBarnepensjonDTO(dto: EtterbetalingDTO) =
        BarnepensjonFrivilligSkattetrekk(
            frivilligSkattetrekk = dto.frivilligSkattetrekk,
        )
}
