package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

interface VilkaarKlient {
    fun vurderVilkaar(vilkaar: String, opplysninger: List<Behandlingsopplysning>): ObjectNode
}

class KtorVilkarClient(private val url: String) : VilkaarKlient {
    override fun vurderVilkaar(vilkaar: String, opplysninger: List<Behandlingsopplysning>): ObjectNode {
        return runBlocking {
            HttpClient(CIO) {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        registerModule(JavaTimeModule())
                    }
                }
            }.post(url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = RequestDto(
                    "barnepensjon:brukerungnok",
                    opplysninger.map { it.opplysning.put("_navn", it.opplysningType) })
            }
        }

    }
}

data class RequestDto(val vilkaar: String, val opplysninger: List<Any>)
