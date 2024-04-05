package no.nav.etterlatte.behandling.selftest

import no.nav.etterlatte.libs.ktor.PingResult
import java.time.LocalTime

data class SelfTestReport(
    val application: String,
    val timestamp: LocalTime,
    val aggregateResult: Int,
    val checks: List<PingResult>,
)
