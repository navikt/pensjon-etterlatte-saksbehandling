package no.nav.etterlatte.trygdetid

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.trygdetid.klienter.Beskrivelse
import no.nav.etterlatte.trygdetid.klienter.KodeverkKlient
import no.nav.etterlatte.trygdetid.klienter.KodeverkResponse
import java.util.concurrent.TimeUnit

class KodeverkService(private val klient: KodeverkKlient) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<CacheKey, KodeverkResponse>()

    suspend fun hentAlleLand(): List<Land> {
        val landkoder = cache.getIfPresent(CacheKey.LANDKODER)
            ?: klient.hentLandkoder().also { cache.put(CacheKey.LANDKODER, it) }

        return landkoder
            .betydninger
            .flatMap { (_, betydninger) -> betydninger }
            .mapNotNull { it.beskrivelser["nb"]?.let { beskrivelse -> Land(it.gyldigFra, it.gyldigTil, beskrivelse) } }
    }
}

private enum class CacheKey {
    LANDKODER
}

data class Land(
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: Beskrivelse
)