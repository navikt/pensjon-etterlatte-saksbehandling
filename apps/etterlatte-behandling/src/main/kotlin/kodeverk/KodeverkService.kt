package no.nav.etterlatte.kodeverk

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
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

    private val cacheArkivtemaer =
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<CacheKey, KodeverkResponse>()

    suspend fun hentAlleLand(brukerTokenInfo: BrukerTokenInfo): List<Land> {
        val landkoder =
            cache.getIfPresent(CacheKey.LANDKODER)
                ?: klient
                    .hent(KodeverkNavn.LANDKODER, brukerTokenInfo)
                    .also { cache.put(CacheKey.LANDKODER, it) }

        return mapLandkoder(landkoder)
    }

    suspend fun hentAlleLandISO2(brukerTokenInfo: BrukerTokenInfo): List<Land> {
        val landkoder =
            cache.getIfPresent(CacheKey.LANDKODER_ISO2)
                ?: klient
                    .hent(KodeverkNavn.LANDKODERISO2, brukerTokenInfo)
                    .also { cache.put(CacheKey.LANDKODER_ISO2, it) }

        return mapLandkoder(landkoder)
    }

    suspend fun hentArkivTemaer(brukerTokenInfo: BrukerTokenInfo): List<Beskrivelse> {
        val arkivtemaer =
            cacheArkivtemaer.getIfPresent(CacheKey.ARKIVTEMAER)
                ?: klient
                    .hent(KodeverkNavn.ARKIVTEMAER, brukerTokenInfo)
                    .also { cacheArkivtemaer.put(CacheKey.ARKIVTEMAER, it) }

        return arkivtemaer.betydninger.map { (tema, betydninger) ->
            Beskrivelse(
                tema,
                betydninger
                    .first()
                    .beskrivelser["nb"]!!
                    .tekst,
            )
        }
    }

    private fun mapLandkoder(response: KodeverkResponse): List<Land> =
        response
            .betydninger
            .filter { (isoLandkode) -> isoLandkode.matches(Regex("[A-Z]+")) }
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

private enum class CacheKey {
    LANDKODER,
    LANDKODER_ISO2,
    ARKIVTEMAER,
}

data class Land(
    val isoLandkode: String,
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: Beskrivelse,
)
