package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.sak.SakId
import kotlin.random.Random

val sakId1 = SakId(1L)
val sakId2 = SakId(2L)
val sakId3 = SakId(3L)

fun randomSakId() = SakId(Random.nextLong(10, Int.MAX_VALUE.toLong()))

fun JsonNode.tilSakId() = SakId(this.asLong())
