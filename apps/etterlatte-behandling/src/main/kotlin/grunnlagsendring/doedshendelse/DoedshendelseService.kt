package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse as PdlDoedshendelse

class DoedshendelseService(
    private val doedshendelseDao: DoedshendelseDao,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreDoedshendelseForBeroertePersoner(doedshendelse: PdlDoedshendelse) {
        logger.info("Mottok dødsmelding fra PDL, finner berørte personer og lagrer ned dødsmelding.")

        val avdoed = pdlTjenesterKlient.hentPdlModell(doedshendelse.fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        if (avdoed.doedsdato == null) {
            sikkerLogg.info("Mottok dødshendelse for ${avdoed.foedselsnummer}, men personen er i live i følge PDL.")
            logger.info("Mottok dødshendelse fra PDL for en levende person. Avbryter. Se sikkerlogg for detaljer.")
            return
        }

        val beroerte = finnBeroerteBarn(avdoed)
        sikkerLogg.info("Fant ${beroerte.size} berørte personer for avdød (${avdoed.foedselsnummer})")

        inTransaction {
            beroerte.forEach { barn ->
                doedshendelseDao.opprettDoedshendelse(
                    Doedshendelse.nyHendelse(
                        avdoedFnr = avdoed.foedselsnummer.verdi.value,
                        avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                        beroertFnr = barn.foedselsnummer.value,
                        relasjon = Relasjon.BARN,
                    ),
                )
            }
        }
    }

    private fun finnBeroerteBarn(avdoed: PersonDTO): List<Person> {
        val maanedenEtterDoedsfall = avdoed.doedsdato!!.verdi.plusMonths(1).withDayOfMonth(1)

        return with(avdoed.avdoedesBarn ?: emptyList()) {
            this.filter { barn -> barn.doedsdato == null }
                .filter { barn -> barn.under20PaaDato(maanedenEtterDoedsfall) }
        }
    }
}

private fun Person.under20PaaDato(dato: LocalDate): Boolean =
    if (foedselsdato != null) {
        ChronoUnit.YEARS.between(foedselsdato, dato) < 20
    } else {
        foedselsnummer.getAgeAtDate(dato) < 20
    }
