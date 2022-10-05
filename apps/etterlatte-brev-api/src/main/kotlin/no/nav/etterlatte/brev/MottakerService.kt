package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate


interface MottakerService {
    suspend fun hentStatsforvalterListe(): List<Enhet>
}

class MottakerServiceImpl(private val httpClient: HttpClient, private val url: String) : MottakerService {
    private val logger = LoggerFactory.getLogger(MottakerService::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1))
        .build<LocalDate, List<Enhet>>()

    override suspend fun hentStatsforvalterListe(): List<Enhet> {
        val enheter = cache.getIfPresent(LocalDate.now())

        return if (!enheter.isNullOrEmpty()) {
            logger.info("Fant cachet liste over statsforvaltere (antall ${enheter.size}")
            enheter
        } else {
            val nyListe = httpClient.get("$url/enheter/statsforvalter").body<List<Enhet>>()

            if (nyListe.isEmpty()) {
                logger.error("Tom liste i respons fra etterlatte-brreg ... ")
                emptyList()
            } else {
                nyListe.also {
                    logger.info("Tom cache, hentet ${it.size} statsforvaltere fra enhetsregisteret")
                    cache.put(LocalDate.now(), it)
                }
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val organisasjonsnummer: String,
    val navn: String
)