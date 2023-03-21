package no.nav.etterlatte.brev.adresse.enhetsregister

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

class BrregService(private val klient: BrregKlient) {
    private val logger = LoggerFactory.getLogger(BrregService::class.java)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofDays(1))
        .build<LocalDate, List<Enhet>>()

    suspend fun hentAlleStatsforvaltere(): List<Enhet> {
        val enheter = cache.getIfPresent(LocalDate.now())

        return if (!enheter.isNullOrEmpty()) {
            logger.info("Fant cachet liste over statsforvaltere (antall ${enheter.size})")
            enheter
        } else {
            val nyListe = klient.hentEnheter()

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