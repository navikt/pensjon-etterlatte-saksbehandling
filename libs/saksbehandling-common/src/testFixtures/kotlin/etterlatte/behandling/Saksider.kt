package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.JsonNode
import kotlin.random.Random

val sakId1 = 1L
val sakId2 = 2L
val sakId3 = 3L

fun randomSakId() = Random.nextLong(10, Int.MAX_VALUE.toLong())

fun tilSakId(long: Long) = long

fun tilSakId(int: Int) = tilSakId(int.toLong())

fun JsonNode.tilSakId() = this.asLong()
