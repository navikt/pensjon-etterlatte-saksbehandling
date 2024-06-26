package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harAktivAdresse
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    private val pesysKlient: PesysKlient,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val kontrollpunktEktefelleService = DoedshendelseKontrollpunktEktefelleService()
    private val kontrollpunktAvdoedService = DoedshendelseKontrollpunktAvdoedService()
    private val kontrollpunktBarnService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)
    private val kontrollpunktOMSService = DoedshendelseKontrollpunktOMSService(pesysKlient, behandlingService)

    fun identifiserKontrollerpunkter(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> =
        when (hendelse.relasjon) {
            Relasjon.BARN -> {
                val (sak, avdoed, barn) = hentDataForBeroert(hendelse, PersonRolle.BARN)

                if (avdoed.doedsdato == null) {
                    listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
                } else {
                    val barnKontrollpunkter = kontrollpunktBarnService.identifiser(hendelse, avdoed, sak, barn)
                    val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                    val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak, avdoed, barn)

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
                    val omsKontrollpunkter = kontrollpunktOMSService.identifiser(hendelse, sak, eps, avdoed)
                    val fellesKontrollpunkts = fellesKontrollpunkter(hendelse, sak, avdoed, eps)

                    ektefelleKontrollpunkter + avdoedKontrollpunkter + omsKontrollpunkter + fellesKontrollpunkts
                }
            }

            Relasjon.SAMBOER -> {
                val (sak, avdoed, samboer) = hentDataForBeroert(hendelse, PersonRolle.GJENLEVENDE)

                if (avdoed.doedsdato == null) {
                    listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
                } else {
                    val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                    val omsKontrollpunkter = kontrollpunktOMSService.identifiser(hendelse, sak, samboer, avdoed)
                    val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak, avdoed, samboer)

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
    ): Triple<Sak?, PersonDTO, PersonDTO> {
        val sakType = hendelse.sakTypeForEpsEllerBarn()
        val sak = sakService.finnSak(hendelse.beroertFnr, sakType)
        val avdoed = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        val gjenlevende = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.beroertFnr, beroert, hendelse.sakTypeForEpsEllerBarn())

        return Triple(sak, avdoed, gjenlevende)
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
        avdoed: PersonDTO,
        gjenlevende: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        val duplikatKontrollpunkt = kontrollerDuplikatHendelse(hendelse, sak)
        val adresseKontrollpunkt = kontrollerAktivAdresse(gjenlevende)
        val haandtertAvPesys = behandletAvPesys(avdoed, gjenlevende)

        return listOfNotNull(duplikatKontrollpunkt, adresseKontrollpunkt, haandtertAvPesys)
    }

    private fun kontrollerAktivAdresse(gjenlevende: PersonDTO): DoedshendelseKontrollpunkt? =
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

    private fun behandletAvPesys(
        avdoed: PersonDTO,
        gjenlevende: PersonDTO,
    ): DoedshendelseKontrollpunkt.TilstoetendeBehandletIPesys? =
        runBlocking {
            try {
                if (pesysKlient.erTilstoetendeBehandlet(gjenlevende.foedselsnummer.verdi.value, avdoed.doedsdato!!.verdi)) {
                    DoedshendelseKontrollpunkt.TilstoetendeBehandletIPesys
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.error("Feil ved kall til Pesys for å sjekke om tilstøtt har blitt behandlet", e)
                null // TODO: Fjern try-catch når vi har testet at grensesnittet fungerer som det skal mot Pesys
            }
        }
}
