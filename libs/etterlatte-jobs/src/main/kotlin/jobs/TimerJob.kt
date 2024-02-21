package no.nav.etterlatte.jobs

import java.util.Timer

interface TimerJob {
    fun schedule(): Timer
}
