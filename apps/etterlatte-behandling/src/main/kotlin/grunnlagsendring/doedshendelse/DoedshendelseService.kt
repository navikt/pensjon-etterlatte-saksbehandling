package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl as PdlDoedshendelse

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

    fun settHendelseTilFerdigOgOppdaterBrevId(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert) =
        doedshendelseDao.oppdaterBrevDistribuertDoedshendelse(doedshendelseBrevDistribuert)

    fun kanBrukeDeodshendelserJob() = featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)

    fun opprettDoedshendelseForBeroertePersoner(doedshendelse: PdlDoedshendelse) {
        logger.info("Mottok dødsmelding fra PDL, finner berørte personer og lagrer ned dødsmelding.")

        val avdoed = pdlTjenesterKlient.hentPdlModell(doedshendelse.fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        if (avdoed.doedsdato == null) {
            sikkerLogg.info("Mottok dødshendelse for ${avdoed.foedselsnummer}, men personen er i live i følge PDL.")
            logger.info("Mottok dødshendelse fra PDL for en levende person. Avbryter. Se sikkerlogg for detaljer.")
            return
        }
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        val doedshendelserForAvdoed = inTransaction { doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr) }
        val beroerte = finnBeroerteBarn(avdoed)

        if (doedshendelserForAvdoed.isEmpty()) {
            sikkerLogg.info("Fant ${beroerte.size} berørte personer for avdød (${avdoed.foedselsnummer})")
            inTransaction {
                lagreDoedshendelser(beroerte, avdoed, doedshendelse.endringstype)
            }
        } else {
            haandterEksisterendeHendelser(doedshendelse, doedshendelserForAvdoed, avdoed, beroerte)
        }
    }

    private fun haandterEksisterendeHendelser(
        doedshendelse: PdlDoedshendelse,
        doedshendelserForAvdoed: List<DoedshendelseInternal>,
        avdoed: PersonDTO,
        beroerte: List<Person>,
    ) {
        when (doedshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                inTransaction {
                    val skalOppdatere = doedshendelserForAvdoed.any { it.status !== Status.FERDIG }
                    if (skalOppdatere) {
                        oppdaterDodshendelser(
                            doedshendelserForAvdoed.map {
                                it.tilOppdatert()
                            },
                        )
                    }
                    haandterNyeBerorte(doedshendelserForAvdoed, beroerte, avdoed, doedshendelse.endringstype)
                }
            }
            Endringstype.ANNULLERT -> {
                inTransaction {
                    oppdaterDodshendelser(
                        doedshendelserForAvdoed.map { it.tilAvbrutt() },
                    )
                }
            }
            Endringstype.OPPHOERT -> throw RuntimeException("Fikk opphør på dødshendelse, skal ikke skje ifølge PDL docs")
        }
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        logger.info("Lagrer ikke duplikat doedshendelse for person ${avdoedFnr.maskerFnr()}")
        sikkerLogg.info("Mottok dødshendelse for $avdoedFnr, er duplikat og lagrer det ikke")
    }

    private fun haandterNyeBerorte(
        doedshendelserForAvdoed: List<DoedshendelseInternal>,
        beroerte: List<Person>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val eksisterendeBeroerte = doedshendelserForAvdoed.map { it.beroertFnr }
        val nyeBeroerte = beroerte.map { it.foedselsnummer.value }.filter { !eksisterendeBeroerte.contains(it) }
        nyeBeroerte.forEach { barn ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = barn,
                    relasjon = Relasjon.BARN,
                    endringstype = endringstype,
                ),
            )
        }
    }

    private fun oppdaterDodshendelser(doedshendelserForAvdoed: List<DoedshendelseInternal>) {
        doedshendelserForAvdoed.forEach {
            doedshendelseDao.oppdaterDoedshendelse(it)
        }
    }

    private fun lagreDoedshendelser(
        beroerte: List<Person>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        beroerte.forEach { barn ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = barn.foedselsnummer.value,
                    relasjon = Relasjon.BARN,
                    endringstype = endringstype,
                ),
            )
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
