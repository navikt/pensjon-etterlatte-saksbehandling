package no.nav.etterlatte.trygdetid

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.etterlatte.trygdetid.klienter.Beskrivelse
import no.nav.etterlatte.trygdetid.klienter.BetydningMedIsoKode
import no.nav.etterlatte.trygdetid.klienter.KodeverkKlient
import no.nav.etterlatte.trygdetid.klienter.KodeverkResponse
import java.util.concurrent.TimeUnit

class KodeverkService(private val klient: KodeverkKlient) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<CacheKey, KodeverkResponse>()

    suspend fun hentAlleLand(): List<Land> {
        val landkoder = cache.getIfPresent(CacheKey.LANDKODER)
            ?: klient.hentLandkoder().also { cache.put(CacheKey.LANDKODER, it) }

        return landkoder
            .betydninger
            .flatMap { (isoLandkode, betydninger) ->
                betydninger.map {
                    BetydningMedIsoKode(
                        gyldigFra = it.gyldigFra,
                        gyldigTil = it.gyldigTil,
                        beskrivelser = it.beskrivelser,
                        isolandkode = isoLandkode
                    )
                }
            }
            .mapNotNull { betydningMedIsoKode ->
                betydningMedIsoKode.beskrivelser["nb"]?.let { beskrivelse ->
                    Land(
                        isoLandkode = betydningMedIsoKode.isolandkode,
                        betydningMedIsoKode.gyldigFra,
                        betydningMedIsoKode.gyldigTil,
                        LandNormalisert.hentBeskrivelse(betydningMedIsoKode.isolandkode)
                            ?.let { Beskrivelse(term = beskrivelse.term, tekst = it) } ?: beskrivelse
                    )
                }
            }
    }
}

private enum class CacheKey {
    LANDKODER
}

data class Land(
    val isoLandkode: String,
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: Beskrivelse
)