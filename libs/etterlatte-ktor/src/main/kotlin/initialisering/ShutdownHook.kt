package no.nav.etterlatte.libs.ktor.initialisering

import java.util.Timer

data class ShutdownHook(val timer: Timer, val action: (Timer) -> Unit)
