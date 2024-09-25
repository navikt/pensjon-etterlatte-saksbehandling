package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.sak.SakId
import kotlin.random.Random

val sakId1 = tilSakId(1L)
val sakId2 = tilSakId(2L)
val sakId3 = tilSakId(3L)

fun randomSakId() = tilSakId(Random.nextLong(10, Int.MAX_VALUE.toLong()))

fun tilSakId(long: Long) = SakId(long)

fun tilSakId(int: Int) = tilSakId(int.toLong())

fun JsonNode.tilSakId() = tilSakId(this.asLong())
