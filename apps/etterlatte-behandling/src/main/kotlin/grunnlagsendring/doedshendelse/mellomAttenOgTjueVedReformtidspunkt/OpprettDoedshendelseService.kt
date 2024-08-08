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
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService.Companion.SENESTE_TIDSPUNKT
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService.Companion.TIDLIGSTE_TIDSPUNKT
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

        val eksisterendeBeroerte =
            inTransaction { doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr) }
                .filter { it.utfall !== Utfall.AVBRUTT }
                .map { it.beroertFnr }

        val nyeBeroerte =
            beroerteBarn
                .map { PersonFnrMedRelasjon(it.fnr, it.relasjon) }
                .filter { !eksisterendeBeroerte.contains(it.fnr) }

        sikkerLogg.info("Fant ${nyeBeroerte.size} berørte personer for avdød (${avdoed.foedselsnummer.verdi.value})")
        logger.info("Fant ${nyeBeroerte.size} berørte personer for avdød (${avdoed.foedselsnummer.verdi.value.maskerFnr()})")
        inTransaction {
            if (featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanLagreDoedshendelse, false)) {
                lagreDoedshendelser(nyeBeroerte, avdoed, Endringstype.OPPRETTET)
            }
        }
    }

    private fun lagreDoedshendelser(
        beroerte: List<PersonFnrMedRelasjon>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        beroerte.forEach { person ->
            sikkerLogg.info("Oppretter dødshendelse for person (${person.fnr})")
            logger.info("Oppretter dødshendelse for person (${person.fnr.maskerFnr()})")
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
                .filter { barn -> barn.merEnnEller18PaaVirkningstidspunkt(avdoed.doedsdato!!.verdi) }
                .filter { barn -> barn.mellom18og20PaaReformtidspunkt() }
                .map { PersonFnrMedRelasjon(it.foedselsnummer.value, Relasjon.BARN) }
        }

    companion object {
        val REFORMTIDSPUNKT = LocalDate.of(2024, 1, 1)
        val TIDLIGSTE_TIDSPUNKT = REFORMTIDSPUNKT.minusYears(20) // 2004.01.01
        val SENESTE_TIDSPUNKT = REFORMTIDSPUNKT.minusYears(18).minusDays(1) // 2005.12.31
    }
}

fun Person.mellom18og20PaaReformtidspunkt(): Boolean {
    // Dersom vi ikke har en fødselsdato antar vi at personen kan ha bursdag på nyttårsaften,
    // for å sikre at vi får med alle som er under 20 år.
    val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)

    // Sjekker at fødselsdato er fra tidligste tidspunkt til og med seneste tidspunkt
    fun mellomAttenOgTjuePaaReformtidspunkt(foedselsdato: LocalDate): Boolean =
        !(foedselsdato.isBefore(TIDLIGSTE_TIDSPUNKT) || foedselsdato.isAfter(SENESTE_TIDSPUNKT))

    return mellomAttenOgTjuePaaReformtidspunkt(benyttetFoedselsdato)
}

fun Person.merEnnEller18PaaVirkningstidspunkt(doedsdato: LocalDate): Boolean {
    // Dersom vi ikke har en fødselsdato antar vi at personen kan ha bursdag på nyttårsaften,
    // for å sikre at vi får med alle som er under 20 år.
    val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)
    val virkningstidspunkt = doedsdato.plusMonths(1).withDayOfMonth(1)

    return ChronoUnit.YEARS.between(benyttetFoedselsdato, virkningstidspunkt).absoluteValue >= 18
}
