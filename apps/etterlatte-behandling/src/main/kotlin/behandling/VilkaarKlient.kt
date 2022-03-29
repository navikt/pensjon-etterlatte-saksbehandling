package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat

interface VilkaarKlient {
    fun vurderVilkaar(opplysninger: List<Behandlingsopplysning<ObjectNode>>): VilkaarResultat
}

class KtorVilkarClient(private val url: String) : VilkaarKlient {
    override fun vurderVilkaar(opplysninger: List<Behandlingsopplysning<ObjectNode>>): VilkaarResultat {
        return runBlocking {
            HttpClient(CIO) {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        registerModule(JavaTimeModule())
                    }
                }
                defaultRequest {
                    header(X_CORRELATION_ID, getCorrelationId())
                }
            }.post(url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = RequestDto(
                    opplysninger.map { VilkaarOpplysning(it.opplysningType, it.kilde, it.opplysning)  })
            }
        }
    }
}

data class RequestDto(val opplysninger: List<VilkaarOpplysning<ObjectNode>>)
