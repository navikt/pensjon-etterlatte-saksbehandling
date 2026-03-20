package no.nav.etterlatte

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import tools.jackson.module.kotlin.readValue

inline fun <reified T> readValue(resource: Resource): T = resource.response.let { objectMapper.readValue(it.toString()) }
