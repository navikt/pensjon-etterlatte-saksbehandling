package no.nav.etterlatte.utbetaling.simulering

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.slf4j.LoggerFactory
import java.io.IOException

private typealias RequestWrapper = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
private typealias ResponseWrapper = no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse

fun simuleringObjectMapper(): ObjectMapper =
    objectMapper
        .copy()
        .registerModule(StringTrimModule())

class SimuleringOsKlient(
    config: Config,
    private val client: HttpClient,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val url = config.getString("etterlatteproxy.url")

    suspend fun simuler(request: SimulerBeregningRequest): SimulerBeregningResponse {
        logger.info("Kaller simuleringstjeneste i Oppdrag (via proxy) request ${request.toJson()}")

        val response =
            client.post("$url/simuleringoppdrag/simulerberegning") {
                contentType(ContentType.Application.Json)
                setBody(
                    no.nav.etterlatte.utbetaling.simulering.RequestWrapper().apply {
                        this.request = request
                    },
                )
            }
        if (!response.status.isSuccess()) {
            throw SimuleringOsKlientException(
                response.status,
                "Simulering mot Oppdrag feilet",
            )
        } else {
            return response.body<String>().let {
                objectMapper.readValue<ResponseWrapper>(it).response
                    ?: SimulerBeregningResponse()
            }
        }
    }
}

class SimuleringOsKlientException(
    statusCode: HttpStatusCode,
    override val message: String,
) : ForespoerselException(
        status = statusCode.value,
        code = "SIMULERING_OPPDRAG_FEIL",
        detail = message,
        meta =
            mapOf(
                "correlation-id" to getCorrelationId(),
                "tidspunkt" to Tidspunkt.now(),
            ),
    )

internal class StringTrimModule : SimpleModule("string-trim-module") {
    init {
        addDeserializer(
            String::class.java,
            object : StdScalarDeserializer<String?>(
                String::class.java,
            ) {
                @Throws(IOException::class)
                override fun deserialize(
                    jsonParser: JsonParser,
                    ctx: DeserializationContext,
                ): String = jsonParser.valueAsString.trim { it <= ' ' }
            },
        )
    }
}
