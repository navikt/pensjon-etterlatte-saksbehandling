package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse as PdlDoedshendelse

enum class DoedshendelseFeatureToggle(private val key: String) : FeatureToggle {
    KanLagreDoedshendelse("pensjon-etterlatte.kan-lage-doedhendelse"),
    ;

    override fun key(): String = key
}

class DoedshendelseService(
    private val doedshendelseDao: DoedshendelseDao,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettDoedshendelseForBeroertePersoner(doedshendelse: PdlDoedshendelse) {
        logger.info("Mottok dødsmelding fra PDL, finner berørte personer og lagrer ned dødsmelding.")

        val avdoed = pdlTjenesterKlient.hentPdlModell(doedshendelse.fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        if (avdoed.doedsdato == null) {
            sikkerLogg.info("Mottok dødshendelse for ${avdoed.foedselsnummer}, men personen er i live i følge PDL.")
            logger.info("Mottok dødshendelse fra PDL for en levende person. Avbryter. Se sikkerlogg for detaljer.")
            return
        }

        val beroerte = finnBeroerteBarn(avdoed)
        sikkerLogg.info("Fant ${beroerte.size} berørte personer for avdød (${avdoed.foedselsnummer})")

        lagreDoedshendelse(beroerte, avdoed)
    }

    private fun lagreDoedshendelse(
        beroerte: List<Person>,
        avdoed: PersonDTO,
    ) {
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)) {
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
    }

    private fun finnBeroerteBarn(avdoed: PersonDTO): List<Person> {
        val maanedenEtterDoedsfall = avdoed.doedsdato!!.verdi.plusMonths(1).withDayOfMonth(1)

        return with(avdoed.avdoedesBarn ?: emptyList()) {
            this.filter { barn -> barn.doedsdato == null }
                .filter { barn -> barn.under20PaaDato(maanedenEtterDoedsfall) }
        }
    }
}

private fun Person.under20PaaDato(dato: LocalDate): Boolean {
    // Dersom vi ikke har en fødselsdato antar vi at personen kan ha bursdag på nyttårsaften,
    // for å sikre at vi får med alle som er under 20 år.
    val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)

    return ChronoUnit.YEARS.between(benyttetFoedselsdato, dato) < 20
}
