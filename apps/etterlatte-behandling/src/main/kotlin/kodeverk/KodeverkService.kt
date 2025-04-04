package no.nav.etterlatte.kodeverk

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.libs.common.kodeverk.BeskrivelseDto
import no.nav.etterlatte.libs.common.kodeverk.LandDto
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

    suspend fun hentAlleLand(brukerTokenInfo: BrukerTokenInfo): List<LandDto> {
        val landkoder =
            cache.getIfPresent(CacheKey.LANDKODER)
                ?: klient
                    .hent(KodeverkNavn.LANDKODER, false, brukerTokenInfo)
                    .also { cache.put(CacheKey.LANDKODER, it) }

        return mapLandkoder(landkoder)
    }

    suspend fun hentAlleLandISO2(brukerTokenInfo: BrukerTokenInfo): List<LandDto> {
        val landkoder =
            cache.getIfPresent(CacheKey.LANDKODER_ISO2)
                ?: klient
                    .hent(KodeverkNavn.LANDKODERISO2, false, brukerTokenInfo)
                    .also { cache.put(CacheKey.LANDKODER_ISO2, it) }

        return mapLandkoder(landkoder)
    }

    suspend fun hentArkivTemaer(brukerTokenInfo: BrukerTokenInfo): List<Beskrivelse> {
        val arkivtemaer =
            cacheArkivtemaer.getIfPresent(CacheKey.ARKIVTEMAER)
                ?: klient
                    .hent(KodeverkNavn.ARKIVTEMAER, true, brukerTokenInfo)
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

    private fun mapLandkoder(response: KodeverkResponse): List<LandDto> =
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
                    LandDto(
                        isoLandkode = betydningMedIsoKode.isolandkode,
                        gyldigFra = betydningMedIsoKode.gyldigFra,
                        gyldigTil = betydningMedIsoKode.gyldigTil,
                        beskrivelse =
                            LandNormalisert
                                .hentBeskrivelse(betydningMedIsoKode.isolandkode)
                                ?.let { BeskrivelseDto(term = beskrivelse.term, tekst = it) }
                                ?: beskrivelse.let { BeskrivelseDto(it.term, it.tekst) },
                    )
                }
            }
}

private enum class CacheKey {
    LANDKODER,
    LANDKODER_ISO2,
    ARKIVTEMAER,
}
