package no.nav.etterlatte.libs.common.person

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun LocalDate.hentAlder(): Int = ChronoUnit.YEARS.between(this, LocalDate.now()).toInt()
