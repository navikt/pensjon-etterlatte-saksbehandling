package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harAktivAdresse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    pesysKlient: PesysKlient,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val kontrollpunktEktefelleService = DoedshendelseKontrollpunktEktefelleService()
    private val kontrollpunktAvdoedService = DoedshendelseKontrollpunktAvdoedService()
    private val kontrollpunktBarnService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)
    private val kontrollpunktOMSService = DoedshendelseKontrollpunktOMSService(pesysKlient, behandlingService)

    fun identifiserKontrollpunkter(
        hendelse: DoedshendelseInternal,
        bruker: BrukerTokenInfo,
    ): List<DoedshendelseKontrollpunkt> =
        when (hendelse.relasjon) {
            Relasjon.BARN -> {
                val (sak, avdoed, barn) = hentDataForBeroert(hendelse, PersonRolle.BARN)

                if (avdoed.doedsdato == null) {
                    listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
                } else {
                    val barnKontrollpunkter = kontrollpunktBarnService.identifiser(hendelse, avdoed, sak, barn)
                    val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                    val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak, barn)

                    barnKontrollpunkter + avdoedKontrollpunkter + fellesKontrollpunkter
                }
            }

            Relasjon.EKTEFELLE -> {
                val (sak, avdoed, eps) = hentDataForBeroert(hendelse, PersonRolle.GJENLEVENDE)

                if (avdoed.doedsdato == null) {
                    listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
                } else {
                    val ektefelleKontrollpunkter = kontrollpunktEktefelleService.identifiser(eps, avdoed)
                    val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                    val omsKontrollpunkter =
                        kontrollpunktOMSService.identifiser(
                            hendelse = hendelse,
                            sak = sak,
                            eps = eps,
                            avdoed = avdoed,
                            bruker = bruker,
                        )
                    val fellesKontrollpunkts = fellesKontrollpunkter(hendelse, sak, eps)

                    ektefelleKontrollpunkter + avdoedKontrollpunkter + omsKontrollpunkter + fellesKontrollpunkts
                }
            }

            Relasjon.SAMBOER -> {
                val (sak, avdoed, samboer) = hentDataForBeroert(hendelse, PersonRolle.GJENLEVENDE)

                if (avdoed.doedsdato == null) {
                    listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
                } else {
                    val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                    val omsKontrollpunkter =
                        kontrollpunktOMSService.identifiser(
                            hendelse = hendelse,
                            sak = sak,
                            eps = samboer,
                            avdoed = avdoed,
                            bruker = bruker,
                        )
                    val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak, samboer)

                    avdoedKontrollpunkter + omsKontrollpunkter + fellesKontrollpunkter
                }
            }

            Relasjon.AVDOED -> {
                kontrollerAvdoedHarYtelseIGjenny(hendelse)
            }
        }

    private fun hentDataForBeroert(
        hendelse: DoedshendelseInternal,
        beroert: PersonRolle,
    ): Triple<Sak?, PersonDoedshendelseDto, PersonDoedshendelseDto> {
        val sakType = hendelse.sakTypeForEpsEllerBarn()
        val sak = hentSakForDoedshendelse(hendelse.beroertFnr, sakType)

        val avdoed = pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        val gjenlevende =
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(hendelse.beroertFnr, beroert, hendelse.sakTypeForEpsEllerBarn())

        return Triple(sak, avdoed, gjenlevende)
    }

    /**
     * Midlertidig løsning inntil vi får nøstet opp i tilfellene hvor det finnes flere saker på én person
     * Gjelder i hovedsak personer som har fått endret identifikator
     **/
    private fun hentSakForDoedshendelse(
        beroertFnr: String,
        sakType: SakType,
    ): Sak? =
        try {
            sakService.finnSak(beroertFnr, sakType)
        } catch (_: InternfeilException) {
            val saker = sakService.finnSaker(beroertFnr)

            if (saker.size > 1) {
                logger.error("Fikk flere saker på ident ved henting av data for dødshendelse. Se sikkerlogg.")
                sikkerLogg.error("Det finnes flere saker på bruker med ident=$beroertFnr: ${saker.joinToString()} ")

                val sakMedBehandlinger = behandlingService.hentSakMedBehandlinger(saker)
                logger.info("Bruker sak ${sakMedBehandlinger.sak.id} for dødshendelsen")

                with(sakMedBehandlinger.sak) {
                    Sak(
                        ident = ident,
                        sakType = sakType,
                        id = id,
                        enhet = enhet,
                        adressebeskyttelse = adressebeskyttelse,
                        erSkjermet = erSkjermet,
                    )
                }
            } else {
                saker.firstOrNull()
            }
        }

    private fun kontrollerAvdoedHarYtelseIGjenny(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> {
        val sakerForAvdoed = sakService.finnSaker(hendelse.avdoedFnr)
        return if (sakerForAvdoed.isEmpty()) {
            listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
        } else if (sakerForAvdoed.size > 1) {
            throw RuntimeException("Person: ${hendelse.avdoedFnr.maskerFnr()} hendelseid: ${hendelse.id} har 2 saker, må sjekkes manuelt")
        } else {
            val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(sakerForAvdoed[0].id)
            if (sisteIverksatteBehandling != null) {
                val duplikat =
                    sakerForAvdoed.firstNotNullOfOrNull {
                        kontrollerDuplikatHendelse(hendelse, it)
                    }
                listOfNotNull(DoedshendelseKontrollpunkt.AvdoedHarYtelse(sakerForAvdoed.first()), duplikat)
            } else {
                listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
            }
        }
    }

    private fun fellesKontrollpunkter(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
        gjenlevende: PersonDoedshendelseDto,
    ): List<DoedshendelseKontrollpunkt> {
        val duplikatKontrollpunkt = kontrollerDuplikatHendelse(hendelse, sak)
        val adresseKontrollpunkt = kontrollerAktivAdresse(gjenlevende)

        return listOfNotNull(duplikatKontrollpunkt, adresseKontrollpunkt)
    }

    private fun kontrollerAktivAdresse(gjenlevende: PersonDoedshendelseDto): DoedshendelseKontrollpunkt? =
        when (harAktivAdresse(gjenlevende)) {
            true -> null
            false -> DoedshendelseKontrollpunkt.GjenlevendeManglerAdresse
        }

    private fun kontrollerDuplikatHendelse(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
    ): DoedshendelseKontrollpunkt? {
        if (sak == null) {
            return null
        }

        val duplikatHendelse =
            grunnlagsendringshendelseDao
                .hentGrunnlagsendringshendelserMedStatuserISak(
                    sakId = sak.id,
                    statuser = listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
                ).filter {
                    it.gjelderPerson == hendelse.avdoedFnr && it.type == GrunnlagsendringsType.DOEDSFALL
                }

        return when {
            duplikatHendelse.isNotEmpty() -> {
                val oppgaver = oppgaveService.hentOppgaverForReferanse(duplikatHendelse.first().id.toString())
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(
                    grunnlagsendringshendelseId = duplikatHendelse.first().id,
                    oppgaveId = oppgaver.firstOrNull()?.id,
                )
            }

            else -> null
        }
    }
}
