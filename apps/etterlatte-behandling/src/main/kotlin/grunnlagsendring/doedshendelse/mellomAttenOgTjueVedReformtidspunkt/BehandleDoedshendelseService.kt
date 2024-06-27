package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt

import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelserKafkaService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.finnOppgaveId
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.finnSak
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.MellomAttenOgTjueVedReformtidspunktFeatureToggle
import no.nav.etterlatte.grunnlagsendring.doedshendelse.personBorIUtlandet
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
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandleDoedshendelseService(
    private val doedshendelseDao: DoedshendelseDao,
    private val doedshendelseKontrollpunktService: BehandleDoedshendelseKontrollpunktService,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val sakService: SakService,
    private val deodshendelserProducer: DoedshendelserKafkaService,
    private val grunnlagService: GrunnlagService,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val krrKlient: KrrKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterDoedshendelse(doedshendelse: DoedshendelseInternal) {
        val kontrollpunkter =
            try {
                doedshendelseKontrollpunktService.identifiserKontrollerpunkter(doedshendelse)
            } catch (e: Exception) {
                val sak = doedshendelse.sakId?.toString() ?: "mangler"
                logger.error("Kunne ikke identifisere kontrollpunkter for sak $sak", e)
                throw e
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
                val sak: Sak? =
                    kontrollpunkter.finnSak()
                        ?: if (featureToggleService.isEnabled(
                                MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanSendeBrevOgOppretteOppgave,
                                false,
                            )
                        ) {
                            opprettSakOgLagGrunnlag(doedshendelse)
                        } else {
                            null
                        }

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
                        sakId = sak?.id,
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
            sakService.finnEllerOpprettSakMedGrunnlag(
                fnr = doedshendelse.beroertFnr,
                type = SakType.BARNEPENSJON,
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

        return kontaktInfo
            ?.spraak
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

    private fun hentAnnenForelder(doedshendelse: DoedshendelseInternal): String? =
        pdlTjenesterKlient
            .hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelse.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            ).familieRelasjon
            ?.verdi
            ?.foreldre
            ?.map { it.value }
            ?.firstOrNull { it != doedshendelse.avdoedFnr }

    private fun sendBrevHvisKravOppfylles(
        doedshendelse: DoedshendelseInternal,
        sak: Sak?,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): Boolean {
        val skalSendeBrev = kontrollpunkter.none { !it.sendBrev }

        if (skalSendeBrev) {
            if (sak != null &&
                featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanSendeBrevOgOppretteOppgave, false)
            ) {
                val borIUtlandet = sjekkUtlandForBeroertIHendelse(doedshendelse)
                logger.info("Sender brev for ${doedshendelse.relasjon.name} for sak ${sak.id}")

                deodshendelserProducer.sendBrevRequestBPMellomAttenOgTjueVedReformtidspunkt(sak, borIUtlandet, erOver18aar = true)
                return true
            } else {
                logger.info("Sender ikke brev for ${doedshendelse.id} for sak fordi feature toggle er av eller mangler sak")
                return true
            }
        }
        return false
    }

    private fun sjekkUtlandForBeroertIHendelse(doedshendelse: DoedshendelseInternal): Boolean {
        val beroertPersonDto =
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelse.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )

        return personBorIUtlandet(beroertPersonDto)
    }

    private fun opprettOppgaveHvisKravOppfylles(
        doedshendelse: DoedshendelseInternal,
        sak: Sak?,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): Pair<Boolean, OppgaveIntern?> {
        val skalOppretteOppgave = kontrollpunkter.any { it.opprettOppgave }

        if (skalOppretteOppgave) {
            if (sak != null &&
                featureToggleService.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanSendeBrevOgOppretteOppgave, false)
            ) {
                val oppgaveTekster = kontrollpunkter.filter { it.opprettOppgave }.map { it.oppgaveTekst }.joinToString(" ")
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
                                kommentar = oppgaveTekster,
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
                logger.info("Oppretter ikke oppgave for ${doedshendelse.id} fordi feature toggle er av")
                return true to null
            }
        }

        return false to null
    }
}
