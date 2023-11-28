package no.nav.etterlatte.migrering.grunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import org.slf4j.LoggerFactory

class Utenlandstilknytningsjekker(private val grunnlagKlient: GrunnlagKlient) {
    val logger = LoggerFactory.getLogger(this::class.java)

    internal fun finnUtenlandstilknytning(request: MigreringRequest): UtlandstilknytningType? {
        val bostedsland = runBlocking { grunnlagKlient.hentBostedsland(request.soeker.value) }

        if (request.enhet.nr !in listOf(Enheter.AALESUND_UTLAND.enhetNr, Enheter.UTLAND.enhetNr)) {
            if (bostedsland.erNorge()) {
                if (request.erFolketrygdberegnet()) {
                    logger.debug("Barnet bor i Norge, og enhet er nasjonal. Saka er folketrygdberegnet. Som forventa for nasjonal")
                    return UtlandstilknytningType.NASJONAL
                } else {
                    logger.debug("Barnet bor i Norge, og enhet er nasjonal. Men saka er ikke folketrygdberegnet. Ikke et forventa scenario")
                }
            } else {
                logger.debug(
                    "Enhet er nasjonal, men barnet bor ikke i Norge: {}. Ikke et forventa scenario.",
                    bostedsland,
                )
            }
        }

        if (request.enhet.nr == Enheter.AALESUND_UTLAND.enhetNr) {
            if (bostedsland.erNorge()) {
                logger.debug("Barnet bor i Norge, og enhet er Ålesund utland. Som forventa for utenlandstilsnitt")
                return UtlandstilknytningType.UTLANDSTILSNITT
            } else {
                logger.debug(
                    "Enhet er Ålesund utland, men barnet bor ikke i Norge: {}. Ikke et forventa scenario.",
                    bostedsland,
                )
            }
        }
        if (request.enhet.nr == Enheter.UTLAND.enhetNr) {
            if (!bostedsland.erNorge()) {
                logger.debug("Barnet bor i ${bostedsland.landkode}, og enhet er utland. Som forventa for bosatt utland")
                return UtlandstilknytningType.BOSATT_UTLAND
            } else {
                logger.debug("Barnet bor i Norge (${bostedsland.landkode}), og enhet er utland. Ikke et forventa scenario")
            }
        }
        logger.debug("Klarte ikke å deterministisk finne utenlandstilknytningstype. Bostedsland var {}", bostedsland)
        return null
    }
}
