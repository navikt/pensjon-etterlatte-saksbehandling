package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseFeatureToggle.KanSendeBrevOgOppretteOppgave
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.finnOppgaveId
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.finnSak
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.token.Fagsaksystem
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class DoedshendelseJobService(
    private val doedshendelseDao: DoedshendelseDao,
    private val doedshendelseKontrollpunktService: DoedshendelseKontrollpunktService,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val sakService: SakService,
    private val dagerGamleHendelserSomSkalKjoeres: Int,
    private val deodshendelserProducer: DoedshendelserKafkaService,
    private val grunnlagService: GrunnlagService,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val krrKlient: KrrKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)) {
            val doedshendelserSomSkalHaanderes =
                inTransaction {
                    val nyeDoedshendelser = hentAlleNyeDoedsmeldinger()
                    logger.info("Antall nye dødsmeldinger ${nyeDoedshendelser.size}")

                    val doedshendelserSomSkalHaanderes = finnGyldigeDoedshendelser(nyeDoedshendelser)
                    logger.info("Antall dødsmeldinger plukket ut for kjøring: ${doedshendelserSomSkalHaanderes.size}")
                    doedshendelserSomSkalHaanderes
                }
            doedshendelserSomSkalHaanderes.forEach { doedshendelse ->
                inTransaction {
                    logger.info("Starter håndtering av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()}")
                    haandterDoedshendelse(doedshendelse)
                }
            }
        }
    }

    private fun haandterDoedshendelse(doedshendelse: DoedshendelseInternal) {
        val kontrollpunkter = doedshendelseKontrollpunktService.identifiserKontrollerpunkter(doedshendelse)
        if (kontrollpunkter.isEmpty()) {
            logger.info("fant ingen kontrollpunkter for dødshendelse ${doedshendelse.sakId}, avbryter")
            doedshendelseDao.oppdaterDoedshendelse(
                doedshendelse.tilAvbrutt(
                    sakId = doedshendelse.sakId!!,
                    kontrollpunkter = kontrollpunkter,
                ),
            )
            return
        }
        when (kontrollpunkter.any { it.avbryt }) {
            true -> {
                logger.info(
                    "Avbryter behandling av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()} med avdød " +
                        "${doedshendelse.avdoedFnr.maskerFnr()} grunnet kontrollpunkt: " +
                        kontrollpunkter.joinToString(","),
                )

                doedshendelseDao.oppdaterDoedshendelse(
                    doedshendelse.tilAvbrutt(
                        sakId = kontrollpunkter.finnSak()?.id,
                        oppgaveId = kontrollpunkter.finnOppgaveId(),
                        kontrollpunkter = kontrollpunkter,
                    ),
                )
            }

            false -> {
                logger.info("Skal håndtere dødshendelse")
                val sak: Sak =
                    kontrollpunkter.finnSak() ?: opprettSakOgLagGrunnlag(doedshendelse)

                val brevSendt = sendBrevHvisKravOppfylles(doedshendelse, sak, kontrollpunkter)
                val (oppgaveOpprettet, oppgave) = opprettOppgaveHvisKravOppfylles(doedshendelse, sak, kontrollpunkter)
                val utfall =
                    if (brevSendt && oppgaveOpprettet) {
                        Utfall.BREV_OG_OPPGAVE
                    } else if (brevSendt) {
                        Utfall.BREV
                    } else if (oppgaveOpprettet) {
                        Utfall.OPPGAVE
                    } else {
                        logger.error("Kan ikke håndtere dødshendelse ${doedshendelse.id}")
                        throw IllegalStateException("Kan ikke ha utfall uten brev eller oppgave")
                    }

                doedshendelseDao.oppdaterDoedshendelse(
                    doedshendelse.tilBehandlet(
                        utfall = utfall,
                        sakId = sak.id,
                        oppgaveId = oppgave?.id,
                        kontrollpunkter = kontrollpunkter,
                    ),
                )
            }
        }
    }

    private fun opprettSakOgLagGrunnlag(doedshendelse: DoedshendelseInternal): Sak {
        logger.info("Oppretter sak for dødshendelse ${doedshendelse.id} avdøed ${doedshendelse.avdoedFnr.maskerFnr()}")
        val opprettetSak =
            sakService.finnEllerOpprettSak(
                fnr = doedshendelse.beroertFnr,
                type = doedshendelse.sakTypeForEpsEllerBarn(),
            )

        val gjenlevende =
            when (opprettetSak.sakType) {
                SakType.BARNEPENSJON -> hentAnnenForelder(doedshendelse)
                SakType.OMSTILLINGSSTOENAD -> null
            }
        val galleri =
            Persongalleri(
                soeker = doedshendelse.beroertFnr,
                avdoed = listOf(doedshendelse.avdoedFnr),
                gjenlevende = listOfNotNull(gjenlevende),
                innsender = Vedtaksloesning.GJENNY.name,
            )

        grunnlagService.leggInnNyttGrunnlagSak(sak = opprettetSak, galleri)
        val kilde = Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now())

        val spraak = hentSpraak(doedshendelse)
        val spraakOpplysning = lagOpplysning(Opplysningstype.SPRAAK, kilde, spraak.verdi.toJsonNode())
        grunnlagService.leggTilNyeOpplysningerBareSak(
            sakId = opprettetSak.id,
            opplysninger = NyeSaksopplysninger(opprettetSak.id, listOf(spraakOpplysning)),
        )
        return opprettetSak
    }

    private fun hentSpraak(doedshendelse: DoedshendelseInternal): Spraak {
        val kontaktInfo =
            runBlocking {
                krrKlient.hentDigitalKontaktinformasjon(doedshendelse.avdoedFnr)
            }

        return kontaktInfo?.spraak
            ?.toLowerCasePreservingASCIIRules()
            ?.let {
                when (it) {
                    "nb" -> Spraak.NB
                    "nn" -> Spraak.NN
                    "en" -> Spraak.EN
                    else -> Spraak.NB
                }
            } ?: Spraak.NB
    }

    private fun hentAnnenForelder(doedshendelse: DoedshendelseInternal): String? {
        return pdlTjenesterKlient.hentPdlModellFlereSaktyper(
            foedselsnummer = doedshendelse.beroertFnr,
            rolle = PersonRolle.BARN,
            saktype = SakType.BARNEPENSJON,
        ).familieRelasjon?.verdi?.foreldre
            ?.map { it.value }
            ?.firstOrNull { it != doedshendelse.avdoedFnr }
    }

    private fun sendBrevHvisKravOppfylles(
        doedshendelse: DoedshendelseInternal,
        sak: Sak,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): Boolean {
        val skalSendeBrev = kontrollpunkter.none { !it.sendBrev }
        if (skalSendeBrev) {
            if (featureToggleService.isEnabled(KanSendeBrevOgOppretteOppgave, false)) {
                logger.info("Sender brev for ${doedshendelse.relasjon.name} for sak ${sak.id}")
                deodshendelserProducer.sendBrevRequest(sak)
                return true
            } else {
                logger.info("Sender ikke brev for ${doedshendelse.relasjon.name} for sak ${sak.id} fordi feature toggle er av")
                return true
            }
        }
        return false
    }

    private fun opprettOppgaveHvisKravOppfylles(
        doedshendelse: DoedshendelseInternal,
        sak: Sak,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): Pair<Boolean, OppgaveIntern?> {
        val skalOppretteOppgave = kontrollpunkter.any { it.opprettOppgave }

        if (skalOppretteOppgave) {
            if (featureToggleService.isEnabled(KanSendeBrevOgOppretteOppgave, false)) {
                logger.info("Oppretter oppgave for ${doedshendelse.relasjon.name} for sak ${sak.id}")
                val oppgave =
                    grunnlagsendringshendelseService.opprettDoedshendelseForPerson(
                        grunnlagsendringshendelse =
                            Grunnlagsendringshendelse(
                                id = UUID.randomUUID(),
                                sakId = sak.id,
                                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                                type = GrunnlagsendringsType.DOEDSFALL,
                                opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                                hendelseGjelderRolle = Saksrolle.AVDOED,
                                gjelderPerson = doedshendelse.avdoedFnr,
                                samsvarMellomKildeOgGrunnlag =
                                    SamsvarMellomKildeOgGrunnlag.Doedsdatoforhold(
                                        fraGrunnlag = null,
                                        fraPdl = doedshendelse.avdoedDoedsdato,
                                        samsvar = false,
                                    ),
                            ),
                    )
                return true to oppgave
            } else {
                logger.info("Oppretter ikke oppgave for ${doedshendelse.relasjon.name} for sak ${sak.id} fordi feature toggle er av")
                return true to null
            }
        }

        return false to null
    }

    private fun finnGyldigeDoedshendelser(hendelser: List<DoedshendelseInternal>): List<DoedshendelseInternal> {
        val idag = LocalDateTime.now()

        val avdoedHendelser = hendelser.filter { it.relasjon == Relasjon.AVDOED }
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= dagerGamleHendelserSomSkalKjoeres
        }.distinctBy { it.avdoedFnr } + avdoedHendelser.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(Status.NY)
}
