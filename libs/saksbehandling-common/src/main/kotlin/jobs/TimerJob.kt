package no.nav.etterlatte.libs.common

import java.util.Timer

interface TimerJob {
    fun schedule(): Timer
}
