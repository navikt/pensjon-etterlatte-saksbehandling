package no.nav.etterlatte.libs.common.tidspunkt

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalUnit
import java.util.*

abstract class TruncatedInstant(
    @JsonValue
    internal val instant: Instant
) : Temporal by instant,
    TemporalAdjuster by instant,
    Comparable<TruncatedInstant>,
    Serializable by instant {

    override fun toString() = instant.toString()
    override fun compareTo(other: TruncatedInstant) = this.instant.compareTo(other.instant)
    fun isBefore(other: TruncatedInstant) = this.instant.isBefore(other.instant)
    fun isAfter(other: TruncatedInstant) = this.instant.isAfter(other.instant)
    fun toEpochMilli() = instant.toEpochMilli()
    fun toLocalDate(): LocalDate = LocalDate.ofInstant(instant, standardTidssoneUTC)
    fun toNorskLocalDate(): LocalDate = LocalDate.ofInstant(instant, norskTidssone)

    fun toLocalTime(): LocalTime = LocalTime.ofInstant(instant, standardTidssoneUTC)

    fun toJavaUtilDate(): Date = Date.from(instant)
}

/**
 * Wraps Instants and truncates them to microsecond-precision (postgres precision).
 * Purpose is to unify the level of precision such that comparison of time behaves as expected on all levels (code/db).
 * Scenarios to avoid includes cases where timestamps of db-queries wraps around to the next day at different times
 * based on the precision at hand - which may lead to rows not being picked up as expected. This case is especially
 * relevant i.e. when combining timestamp-db-fields (truncated by db) with Instants stored as json (not truncated by db).
 */
class Tidspunkt
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(
    instant: Instant
) : TruncatedInstant(instant.truncatedTo(unit)) {

    companion object {
        val unit: ChronoUnit = ChronoUnit.MICROS
        val MIN = Tidspunkt(Instant.EPOCH)
        fun now(clock: Clock = utcKlokke()) = Tidspunkt(Instant.now(clock))

        fun from(clock: Clock = utcKlokke()) = Tidspunkt(clock.instant())
        fun parse(text: String) = Tidspunkt(Instant.parse(text))

        fun ofNorskTidssone(dato: LocalDate, tid: LocalTime) =
            Tidspunkt(ZonedDateTime.of(dato, tid, norskTidssone).toInstant())
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
    override fun plus(amount: Long, unit: TemporalUnit): Tidspunkt = Tidspunkt(instant.plus(amount, unit))
    override fun minus(amount: Long, unit: TemporalUnit): Tidspunkt = Tidspunkt(instant.minus(amount, unit))
    fun medTimeMinuttSekund(tid: LocalTime): Tidspunkt = Tidspunkt(
        ZonedDateTime.ofInstant(instant, standardTidssoneUTC)
            .withHour(tid.hour)
            .withMinute(tid.minute)
            .withSecond(tid.second)
            .toInstant()
    )
}