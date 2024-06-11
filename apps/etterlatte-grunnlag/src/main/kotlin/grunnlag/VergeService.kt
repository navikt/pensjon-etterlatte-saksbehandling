package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.grunnlag.adresse.REGOPPSLAG_ADRESSE
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.BrevMottaker
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.hentRelevantVerge
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class VergeService(
    private val persondataKlient: PersondataKlient,
) {
    private val logger = LoggerFactory.getLogger(VergeService::class.java)

    private val sikkerLogg: Logger = sikkerlogger()

    fun hentGrunnlagsopplysningVergesAdresse(pdlPerson: Person): Grunnlagsopplysning<BrevMottaker>? {
        val pdlVerge = finnVerge(pdlPerson) ?: return null
        val vergesAdresse = hentVergesAdresse(pdlVerge)
        if (vergesAdresse == null) {
            logger.warn("Fant ikke verges adresse ved oppretting av grunnlag, for fnr ${pdlPerson.foedselsnummer}")
        }
        return vergesAdresse
    }

    private fun finnVerge(pdlPerson: Person): VergemaalEllerFremtidsfullmakt? {
        val vergeListe = pdlPerson.vergemaalEllerFremtidsfullmakt
        if ((vergeListe?.size ?: 0) > 1) {
            sikkerLogg.warn("Flere verger for fødselsnummer " + pdlPerson.foedselsnummer)
        }
        return hentRelevantVerge(vergeListe, pdlPerson.foedselsnummer)
    }

    private fun hentVergesAdresse(relevantVerge: VergemaalEllerFremtidsfullmakt): Grunnlagsopplysning<BrevMottaker>? {
        val vergesFnr = relevantVerge.vergeEllerFullmektig.motpartsPersonident!!.value
        val pdlVergeAdresse = persondataKlient.hentAdresseGittFnr(vergesFnr) ?: return null
        val brevMottaker = pdlVergeAdresse.tilFrittstaendeBrevMottaker(vergesFnr)
        if (brevMottaker.adresseTypeIKilde != REGOPPSLAG_ADRESSE) {
            logger.error("Adressetype er ikke regoppslag, noe er feil")
            return null
        }
        return brevMottaker.tilGrunnlagsopplysning(registersReferanse = vergesFnr)
    }

    private fun BrevMottaker.tilGrunnlagsopplysning(registersReferanse: String?): Grunnlagsopplysning<BrevMottaker> =
        Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde =
                Grunnlagsopplysning.Persondata(
                    tidspunktForInnhenting = Tidspunkt.now(),
                    registersReferanse = registersReferanse,
                    opplysningId = null,
                ),
            opplysningType = Opplysningstype.VERGES_ADRESSE,
            meta = objectMapper.createObjectNode(),
            opplysning = this,
            fnr = null,
            periode = null,
        )
}
