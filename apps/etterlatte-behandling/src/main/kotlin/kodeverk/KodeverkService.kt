package no.nav.etterlatte.kodeverk

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.concurrent.TimeUnit

class KodeverkService(
    private val klient: KodeverkKlient,
) {
    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<CacheKey, KodeverkResponse>()

    suspend fun hentAlleLand(brukerTokenInfo: BrukerTokenInfo): List<Land> {
        val landkoder =
            cache.getIfPresent(CacheKey.LANDKODER)
                ?: klient.hentLandkoder(brukerTokenInfo).also { cache.put(CacheKey.LANDKODER, it) }

        return landkoder
            .betydninger
            .flatMap { (isoLandkode, betydninger) ->
                betydninger.map {
                    BetydningMedIsoKode(
                        gyldigFra = it.gyldigFra,
                        gyldigTil = it.gyldigTil,
                        beskrivelser = it.beskrivelser,
                        isolandkode = isoLandkode,
                    )
                }
            }.mapNotNull { betydningMedIsoKode ->
                betydningMedIsoKode.beskrivelser["nb"]?.let { beskrivelse ->
                    Land(
                        isoLandkode = betydningMedIsoKode.isolandkode,
                        gyldigFra = betydningMedIsoKode.gyldigFra,
                        gyldigTil = betydningMedIsoKode.gyldigTil,
                        beskrivelse =
                            LandNormalisert
                                .hentBeskrivelse(betydningMedIsoKode.isolandkode)
                                ?.let { Beskrivelse(term = beskrivelse.term, tekst = it) } ?: beskrivelse,
                    )
                }
            }
    }
}

private enum class CacheKey {
    LANDKODER,
}

data class Land(
    val isoLandkode: String,
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: Beskrivelse,
)
