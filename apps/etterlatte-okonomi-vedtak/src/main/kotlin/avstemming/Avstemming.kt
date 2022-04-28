package no.nav.etterlatte.avstemming

import java.time.LocalDateTime

sealed class Avstemming(
    open val opprettet: LocalDateTime,
    open val avstemmingsnokkelTilOgMed: LocalDateTime,
    open val antallAvstemteOppdrag: Int
)

data class NyAvstemming(
    override val opprettet: LocalDateTime = LocalDateTime.now(),
    override val avstemmingsnokkelTilOgMed: LocalDateTime,
    override val antallAvstemteOppdrag: Int
) : Avstemming(opprettet, avstemmingsnokkelTilOgMed, antallAvstemteOppdrag)

data class FullfortAvstemming(
    val id: Long,
    override val opprettet: LocalDateTime = LocalDateTime.now(),
    override val avstemmingsnokkelTilOgMed: LocalDateTime,
    override val antallAvstemteOppdrag: Int
) : Avstemming(opprettet, avstemmingsnokkelTilOgMed, antallAvstemteOppdrag)

