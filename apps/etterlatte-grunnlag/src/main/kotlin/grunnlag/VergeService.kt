package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.grunnlag.adresse.PersondataAdresse
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

    val sikkerLogg: Logger = sikkerlogger()

    fun hentGrunnlagsopplysningVergesAdresse(pdlPerson: Person): Grunnlagsopplysning<BrevMottaker>? {
        val vergeListe = pdlPerson.vergemaalEllerFremtidsfullmakt
        if ((vergeListe?.size ?: 0) > 1) {
            sikkerLogg.warn("Flere verger for fødselsnummer " + pdlPerson.foedselsnummer.value)
        }
        val pdlVerge = hentRelevantVerge(vergeListe)
        return pdlVerge?.let { verge ->
            val vergesAdresse = hentVergesAdresse(pdlPerson.foedselsnummer.value, verge)
            if (vergesAdresse == null) {
                logger.error(
                    "Fant ikke verges adresse " +
                        "ved oppretting av grunnlag, for fnr ${pdlPerson.foedselsnummer}",
                )
            }
            vergesAdresse
        }
    }

    private fun hentVergesAdresse(
        soekerFnr: String,
        relevantVerge: VergemaalEllerFremtidsfullmakt,
    ): Grunnlagsopplysning<BrevMottaker>? {
        val vergesAdresseInfo = persondataKlient.hentVergeadresseGittVergehaversFnr(soekerFnr)

        return if (vergesAdresseInfo != null) {
            tilBrevMottaker(vergesAdresseInfo, relevantVerge)
                .tilGrunnlagsopplysning(registersReferanse = "$soekerFnr.verge")
        } else {
            val vergesFnr = relevantVerge.vergeEllerFullmektig.motpartsPersonident!!.value
            val pdlVergeAdresse = persondataKlient.hentAdresseGittFnr(vergesFnr)
            return pdlVergeAdresse?.let {
                tilBrevMottaker(pdlVergeAdresse, relevantVerge)
                    .tilGrunnlagsopplysning(registersReferanse = vergesFnr)
            }
        }
    }

    private fun tilBrevMottaker(
        vergesAdresseInfo: PersondataAdresse,
        relevantVerge: VergemaalEllerFremtidsfullmakt,
    ): BrevMottaker {
        val pdlVergeFoedselsnummer = relevantVerge.vergeEllerFullmektig.motpartsPersonident!!.value
        return vergesAdresseInfo.tilFrittstaendeBrevMottaker(pdlVergeFoedselsnummer)
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
