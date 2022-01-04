package no.nav.etterlatte.vilkaar.model

import java.time.LocalDate

class Tidslinje<T>(vararg knekkpunkt: Pair<LocalDate, T>){

    val knekkpunkt = knekkpunkt.asList().sortedBy { it.first }
    private fun knekkpunktDatoer(): Set<LocalDate> = knekkpunkt.map { it.first }.toSet()

    operator fun get(tidspunkt: LocalDate): T? = knekkpunkt.lastOrNull { !it.first.isAfter(tidspunkt) }?.second
    operator fun <U> plus(that: Tidslinje<U>): Tidslinje<Pair<T?, U?>> = zip(that)
    fun <U> map(mapper: (LocalDate, T)->U): Tidslinje<U> =
        Tidslinje(*knekkpunkt.map{it.first to mapper(it.first, it.second)}.toTypedArray())

    fun normaliser(): Tidslinje<T>{
        return Tidslinje(*knekkpunkt.fold(emptyList<Pair<LocalDate, T>>()){ acc, cur ->
            if(acc.isEmpty()) listOf(cur)
            else if (acc.last().second == cur.second) acc
            else acc + cur
        }.toTypedArray())
    }

    fun size() = knekkpunkt.size

    fun <U> zip(other: Tidslinje<U>):Tidslinje<Pair<T?, U?>>{
        return (knekkpunktDatoer() union other.knekkpunktDatoer())
            .map { it to (this[it] to other[it]) }
            .let { Tidslinje(*it.toTypedArray()) }
    }

    override fun toString(): String {
        return knekkpunkt.joinToString(" -> ","[", " >>"){
            "${it.first}:${it.second}"
        }
    }

}