package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource

inline fun <reified T> readValue(resource: Resource): T = resource.response.let { objectMapper.readValue(it.toString()) }
