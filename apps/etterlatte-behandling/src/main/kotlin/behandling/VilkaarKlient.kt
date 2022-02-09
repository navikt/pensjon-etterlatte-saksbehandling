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
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning

interface VilkaarKlient {
    fun vurderVilkaar(vilkaar: String, opplysninger: List<Behandlingsopplysning<ObjectNode>>): ObjectNode
}

class KtorVilkarClient(private val url: String) : VilkaarKlient {
    override fun vurderVilkaar(vilkaar: String, opplysninger: List<Behandlingsopplysning<ObjectNode>>): ObjectNode {
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
                    "barnepensjon:forstegangsbehandling",
                    // opplysninger.map { it.opplysning.put("_navn", it.opplysningType) })
                    opplysninger.map { VilkaarOpplysning(it.opplysningType, it.kilde, it.opplysning)  })
            }
        }

    }
}

fun <T> mapTilVilkaarsOpplysning(opplysning: Behandlingsopplysning<T>): VilkaarOpplysning<T>  {
    return VilkaarOpplysning(
        opplysning.opplysningType,
        opplysning.kilde,
        opplysning.opplysning
    )
}

data class RequestDto(val vilkaar: String, val opplysninger: List<VilkaarOpplysning<Any>>)
