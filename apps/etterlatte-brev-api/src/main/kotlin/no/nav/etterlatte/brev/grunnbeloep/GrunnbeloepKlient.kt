package no.nav.etterlatte.brev.grunnbeloep

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

class GrunnbeloepKlient(private val klient: HttpClient) {
    private val logger = LoggerFactory.getLogger(GrunnbeloepKlient::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1))
        .build<String, Grunnbeloep>()

    suspend fun hentGrunnbeloep(): Grunnbeloep {
        val today = LocalDate.now().toString()

        return try {
            cache.getIfPresent(today) ?: klient.get("https://g.nav.no/api/v1/grunnbeloep").body<Grunnbeloep>().also {
                cache.put(today, it)
            }
        } catch (e: Exception) {
            logger.error("En feil oppsto ved henting av grunnbeløp: ", e)
            throw e
        }
    }
}

data class Grunnbeloep(
    val dato: String,
    val grunnbeloep: Int,
    val grunnbeloepPerMaaned: Int,
    val gjennomsnittPerAar: Int,
    val omregningsfaktor: Double,
    val virkningstidspunktForMinsteinntekt: String
)