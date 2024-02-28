package no.nav.etterlatte.grunnlagsendring.doedshendelse

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
                val oppgave = opprettOppgaveHvisKravOppfylles(doedshendelse, sak, kontrollpunkter)
                val utfall =
                    if (brevSendt && oppgave != null) {
                        Utfall.BREV_OG_OPPGAVE
                    } else if (brevSendt) {
                        Utfall.BREV
                    } else if (oppgave != null) {
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
                type = doedshendelse.sakType(),
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
        // TODO: må gå mot KRR gjøres i EY-3588
        val spraakOpplysning = lagOpplysning(Opplysningstype.SPRAAK, kilde, Spraak.NB.verdi.toJsonNode())
        grunnlagService.leggTilNyeOpplysningerBareSak(
            sakId = opprettetSak.id,
            opplysninger = NyeSaksopplysninger(opprettetSak.id, listOf(spraakOpplysning)),
        )
        return opprettetSak
    }

    private fun hentAnnenForelder(doedshendelse: DoedshendelseInternal): String? {
        return pdlTjenesterKlient.hentPdlModell(
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
            if (doedshendelse.relasjon == Relasjon.BARN) {
                logger.info("Sender brev for ${Relasjon.BARN.name} for sak ${sak.id}")
                deodshendelserProducer.sendBrevRequest(sak)
            }
            return true
            // TODO: EY-3470 relasjon EPS
        }
        return false
    }

    private fun opprettOppgaveHvisKravOppfylles(
        doedshendelse: DoedshendelseInternal,
        sak: Sak,
        kontrollpunkter: List<DoedshendelseKontrollpunkt>,
    ): OppgaveIntern? {
        val skalOppretteOppgave = kontrollpunkter.any { it.opprettOppgave }
        return if (skalOppretteOppgave) {
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
        } else {
            null
        }
    }

    private fun finnGyldigeDoedshendelser(hendelser: List<DoedshendelseInternal>): List<DoedshendelseInternal> {
        val idag = LocalDateTime.now()
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= dagerGamleHendelserSomSkalKjoeres
        }.distinctBy { it.avdoedFnr }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(Status.NY)
}
