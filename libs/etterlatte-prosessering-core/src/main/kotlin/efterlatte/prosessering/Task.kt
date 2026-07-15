package efterlatte.prosessering

import java.time.Instant

data class Task(
    val id: Long,
    val type: String,
    val status: Status,
    val payload: String?,
    val triggerTid: Instant,
    val opprettetTid: Instant,
    val plukketTid: Instant?,
    val antallFeil: Int,
    val stoppaarsak: Stoppaarsak?,
    val versjon: Long,
)
