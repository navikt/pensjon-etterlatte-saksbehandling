package no.nav.etterlatte.libs.common.tidspunkt

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalUnit

abstract class TruncatedInstant(
    @JsonValue
    val instant: Instant
) : Temporal by instant,
    TemporalAdjuster by instant,
    Comparable<Instant> by instant,
    Serializable by instant {

    override fun toString() = instant.toString()
}

/**
 * Wraps Instants and truncates them to microsecond-precision (postgres precision).
 * Purpose is to unify the level of precision such that comparison of time behaves as expected on all levels (code/db).
 * Scenarios to avoid includes cases where timestamps of db-queries wraps around to the next day at different times
 * based on the precision at hand - which may lead to rows not being picked up as expected. This case is especially
 * relevant i.e when combining timestamp-db-fields (truncated by db) with Instants stored as json (not truncated by db).
 */
class Tidspunkt
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(
    instant: Instant
) : TruncatedInstant(instant.truncatedTo(unit)) {

    companion object {
        val unit: ChronoUnit = ChronoUnit.MICROS
        fun now(clock: Clock = klokke()) = Tidspunkt(Instant.now(clock))
    }

    /**
     * Only supports one-way equality check against Instants ("this equals someInstant").
     * Equality check for "someInstant equals this" must be performed by using the wrapped value.
     */
    override fun equals(other: Any?) = when (other) {
        is Tidspunkt -> instant == other.instant
        is Instant -> instant == other.truncatedTo(unit)
        else -> false
    }

    override fun hashCode() = instant.hashCode()
    override fun plus(amount: Long, unit: TemporalUnit): Tidspunkt = instant.plus(amount, unit).toTidspunkt()
    override fun minus(amount: Long, unit: TemporalUnit): Tidspunkt = instant.minus(amount, unit).toTidspunkt()
}

fun Instant.toTidspunkt() = Tidspunkt(this)
fun ZonedDateTime.toTidspunkt() = toInstant().toTidspunkt()
fun LocalDateTime.toTidspunkt(zoneId: ZoneId = standardTidssoneUTC) = atZone(zoneId).toTidspunkt()
fun LocalDateTime.toNorskTidspunkt() = atZone(norskTidssone).toTidspunkt()
fun Tidspunkt.toNorskTid(): ZonedDateTime = ZonedDateTime.ofInstant(this.instant, norskTidssone)
fun Tidspunkt.toTimestamp(): Timestamp = Timestamp.from(this.instant)
fun Timestamp.toTidspunkt(): Tidspunkt = Tidspunkt(this.toInstant())