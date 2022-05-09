package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalUnit

abstract class TruncatedInstant(
    @JsonValue
    val instant: Instant,
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
class Tidspunkt @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    instant: Instant,
) : TruncatedInstant(instant.truncatedTo(unit)) {

    companion object {
        val unit: ChronoUnit = ChronoUnit.MICROS
        fun now(clock: Clock = Clock.systemUTC()) = Tidspunkt(Instant.now(clock))
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
    fun toLocalDate(zoneId: ZoneId): LocalDate = LocalDate.ofInstant(instant, zoneId)
    fun toNorskTid(): LocalDateTime = LocalDateTime.ofInstant(this.instant, ZoneId.of("Europe/Oslo"))
    fun toZonedNorskTid(): ZonedDateTime = ZonedDateTime.ofInstant(this.instant, ZoneId.of("Europe/Oslo"))
    fun plusUnits(units: Int): Tidspunkt = this.plus(units.toLong(), unit)


}

fun Instant.toTidspunkt() = Tidspunkt(this)
fun LocalDateTime.toTidspunkt(zoneId: ZoneId) = this.atZone(zoneId).toTidspunkt()
fun ZonedDateTime.toTidspunkt() = this.toInstant().toTidspunkt()
fun tidssoneNorge() = ZoneId.of("Europe/Oslo")


