package no.nav.etterlatte.enhetsregister

import org.slf4j.LoggerFactory

class EnhetsregService(private val klient: EnhetsregKlient) {
    private val logger = LoggerFactory.getLogger(EnhetsregService::class.java)

    suspend fun hentEnheter(navn: String): List<Enhet> {
        require(navn.isNotBlank()) { "Navn kan ikke ha tom verdi" }

        return klient.hentEnheter(navn)
            .also { logger.info("Fant ${it.size} enheter i BRREG som matchet navn \"$navn\"") }
    }

    suspend fun hentEnhet(orgnr: String): Enhet? {
        val enhet = klient.hentEnhet(orgnr)

        if (enhet == null) {
            logger.info("Fant ingen enhet med orgnr: $orgnr")
        } else {
            logger.info("Fant enhet \"${enhet.navn}\" med orgnr: $orgnr")
        }

        return enhet
    }

    suspend fun hentStatsforvalterListe(): List<Enhet> {
        return klient.hentAlleStatsforvaltere().also {
            logger.info("Fant ${it.size} enheter som matcher 'statsforvalteren' i BRREG!")
        }
    }
}