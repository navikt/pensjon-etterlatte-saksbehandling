package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt

import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.PersonFnrMedRelasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

enum class MellomAttenOgTjueVedReformtidspunktFeatureToggle(
    private val key: String,
) : FeatureToggle {
    KanLagreDoedshendelse("pensjon-etterlatte.kan-lage-doedhendelse-mellom-atten-og-tjue-ved-reformtidspunkt"),
    KanSendeBrevOgOppretteOppgave("pensjon-etterlatte.kan-sende-brev-og-opprette-oppgave-mellom-atten-og-tjue-ved-reformtidspunkt"),
    ;

    override fun key(): String = key
}

class OpprettDoedshendelseService(
    private val doedshendelseDao: DoedshendelseDao,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettDoedshendelse(fnr: String) {
        logger.info("Mottok dødsmelding fra PDL (via uttrekk), finner berørte personer og lagrer ned dødsmelding.")

        val avdoed =
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                fnr,
                PersonRolle.AVDOED,
                listOf(SakType.BARNEPENSJON),
            )

        if (avdoed.doedsdato == null) {
            sikkerLogg.info("Mottok dødshendelse for ${avdoed.foedselsnummer}, men personen er i live i følge PDL.")
            logger.info("Mottok dødshendelse fra PDL for en levende person. Avbryter. Se sikkerlogg for detaljer.")
            return
        }

        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        val beroerteBarn = finnBeroerteBarn(avdoed)

        if (beroerteBarn.isEmpty()) {
            logger.info("Avdøde har ingen berørte barn. Avbryter.")
            return
        }

        val gyldigeDoedshendelserForAvdoed =
            inTransaction { doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr) }
                .filter { it.utfall !== Utfall.AVBRUTT }

        if (gyldigeDoedshendelserForAvdoed.isEmpty()) {
            sikkerLogg.info("Fant ${beroerteBarn.size} berørte personer for avdød (${avdoed.foedselsnummer.verdi.value})")
            logger.info("Fant ${beroerteBarn.size} berørte personer for avdød (${avdoed.foedselsnummer.verdi.value.maskerFnr()})")
            if (featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanLagreDoedshendelse, false)) {
                inTransaction {
                    lagreDoedshendelser(beroerteBarn, avdoed, Endringstype.OPPRETTET)
                }
            }
        } else {
            sikkerLogg.info("Fant ${beroerteBarn.size} nye berørte personer for avdød (${avdoed.foedselsnummer.verdi.value})")
            logger.info("Fant ${beroerteBarn.size} nye berørte personer for avdød (${avdoed.foedselsnummer.verdi.value.maskerFnr()})")
            if (featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanLagreDoedshendelse, false)) {
                inTransaction {
                    lagreDoedshendelserForNyeBeroerte(gyldigeDoedshendelserForAvdoed, beroerteBarn, avdoed, Endringstype.OPPRETTET)
                }
            }
        }
    }

    private fun lagreDoedshendelserForNyeBeroerte(
        doedshendelserForAvdoed: List<DoedshendelseInternal>,
        beroerte: List<PersonFnrMedRelasjon>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val eksisterendeBeroerte = doedshendelserForAvdoed.map { it.beroertFnr }
        val nyeBeroerte =
            beroerte
                .map { PersonFnrMedRelasjon(it.fnr, it.relasjon) }
                .filter { !eksisterendeBeroerte.contains(it.fnr) }
        nyeBeroerte.forEach { person ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = person.fnr,
                    relasjon = person.relasjon,
                    endringstype = endringstype,
                    migrertMellomAttenOgTjue = true,
                ),
            )
        }
    }

    private fun lagreDoedshendelser(
        beroerte: List<PersonFnrMedRelasjon>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        beroerte.forEach { person ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoedFnr,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = person.fnr,
                    relasjon = person.relasjon,
                    endringstype = endringstype,
                    migrertMellomAttenOgTjue = true,
                ),
            )
        }
    }

    private fun finnBeroerteBarn(avdoed: PersonDTO): List<PersonFnrMedRelasjon> =
        with(avdoed.avdoedesBarn ?: emptyList()) {
            this
                .filter { barn -> barn.doedsdato == null }
                .filter { barn -> barn.mellom18og20PaaDato(REFORMTIDSPUNKT) }
                .map { PersonFnrMedRelasjon(it.foedselsnummer.value, Relasjon.BARN) }
        }

    private fun Person.mellom18og20PaaDato(dato: LocalDate): Boolean {
        // Dersom vi ikke har en fødselsdato antar vi at personen kan ha bursdag på nyttårsaften,
        // for å sikre at vi får med alle som er under 20 år.
        val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)

        val mellom18og20PaaDato = ChronoUnit.YEARS.between(benyttetFoedselsdato, dato).absoluteValue in 18..19
        val fyller20IJanuar = (
            ChronoUnit.YEARS.between(benyttetFoedselsdato, dato).absoluteValue == 20L &&
                benyttetFoedselsdato.month == dato.month
        )

        return mellom18og20PaaDato || fyller20IJanuar
    }

    companion object {
        val REFORMTIDSPUNKT = LocalDate.of(2024, 1, 1)
    }
}
