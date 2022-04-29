package no.nav.etterlatte.avstemming

import java.time.LocalDateTime
import java.util.*

data class Avstemming(
    val id: UUID = UUID.randomUUID(),
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val avstemmingsnokkelTilOgMed: LocalDateTime,
    val antallAvstemteOppdrag: Int
)