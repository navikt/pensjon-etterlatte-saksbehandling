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
            .build<KodeverkNavn, KodeverkResponse>()

    suspend fun hentAlleLand(brukerTokenInfo: BrukerTokenInfo): List<Land> {
        val landkoder = hent(KodeverkNavn.LANDKODER, brukerTokenInfo)

        return mapLandkoder(landkoder)
    }

    suspend fun hentAlleLandISO2(brukerTokenInfo: BrukerTokenInfo): List<Land> {
        val landkoder = hent(KodeverkNavn.LANDKODERISO2, brukerTokenInfo)

        return mapLandkoder(landkoder)
    }

    suspend fun hentAlleOppgavetyper(brukerTokenInfo: BrukerTokenInfo): Map<String, String> {
        val oppgavetyper = hent(KodeverkNavn.OPPGAVETYPER, brukerTokenInfo)

        return oppgavetyper.betydninger.mapValues { (_, betydninger) ->
            betydninger
                .first()
                .beskrivelser["nb"]!!
                .let {
                    it.tekst.takeUnless(String::isBlank) ?: it.term
                }
        }
    }

    suspend fun hentArkivTemaer(brukerTokenInfo: BrukerTokenInfo): List<Beskrivelse> {
        val arkivtemaer = hent(KodeverkNavn.ARKIVTEMAER, brukerTokenInfo)

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

    private suspend fun hent(
        kodeverkNavn: KodeverkNavn,
        brukerTokenInfo: BrukerTokenInfo,
    ): KodeverkResponse =
        cache.getIfPresent(kodeverkNavn)
            ?: klient
                .hent(kodeverkNavn, brukerTokenInfo)
                .also { cache.put(kodeverkNavn, it) }

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

data class Land(
    val isoLandkode: String,
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelse: Beskrivelse,
)
