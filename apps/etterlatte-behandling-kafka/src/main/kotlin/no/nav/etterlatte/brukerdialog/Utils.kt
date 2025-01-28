package no.nav.etterlatte.brukerdialog

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

fun formatertTidspunkt(tidspunkt: Instant) =
    with(LocalDateTime.ofInstant(tidspunkt, ZoneOffset.ofHours(0))) {
        "${t(dayOfMonth)}.${t(monthValue)}.$year ${t(hour)}:${t(minute)}:${t(second)}"
    }

fun LocalDate.formatert() = "${t(dayOfMonth)}.${t(monthValue)}.$year"

private fun t(tall: Int) = if (tall < 10) "0$tall" else "$tall"
