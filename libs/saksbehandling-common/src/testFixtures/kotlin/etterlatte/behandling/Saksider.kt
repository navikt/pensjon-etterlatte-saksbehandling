package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.sak.SakId
import tools.jackson.databind.JsonNode
import kotlin.random.Random

val sakId1 = SakId(1L)
val sakId2 = SakId(2L)
val sakId3 = SakId(3L)

fun randomSakId() = SakId(Random.nextLong(10, Int.MAX_VALUE.toLong()))

fun JsonNode.tilSakId() = SakId(this.asLong())

// Jackson 2 overload for rapids-and-rivers test compatibility
@JvmName("tilSakIdJackson2")
fun com.fasterxml.jackson.databind.JsonNode.tilSakId() = SakId(this.asLong())
