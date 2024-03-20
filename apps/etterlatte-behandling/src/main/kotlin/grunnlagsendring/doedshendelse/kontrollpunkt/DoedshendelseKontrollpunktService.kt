package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    private val pesysKlient: PesysKlient,
    private val behandlingService: BehandlingService,
) {
    private val kontrollpunktEktefelleService = DoedshendelseKontrollpunktEktefelleService()
    private val kontrollpunktAvdoedService = DoedshendelseKontrollpunktAvdoedService()
    private val kontrollpunktBarnService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)
    private val kontrollpunktOMSService = DoedshendelseKontrollpunktOMSService(pesysKlient, behandlingService)

    fun identifiserKontrollerpunkter(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> =
        when (hendelse.relasjon) {
            Relasjon.BARN -> {
                val (sak, avdoed) = hentDataForBeroert(hendelse)
                val barnKontrollpunkter = kontrollpunktBarnService.identifiser(hendelse, avdoed, sak)
                val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak)

                barnKontrollpunkter + avdoedKontrollpunkter + fellesKontrollpunkter
            }

            Relasjon.EKTEFELLE -> {
                val (sak, avdoed) = hentDataForBeroert(hendelse)
                val eps =
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = hendelse.beroertFnr,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = hendelse.sakTypeForEpsEllerBarn(),
                    )

                val ektefelleKontrollpunkter = kontrollpunktEktefelleService.identifiser(eps, avdoed)
                val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                val omsKontrollpunkter = kontrollpunktOMSService.identifiser(hendelse, sak, eps, avdoed)
                val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak) // todo: rename

                ektefelleKontrollpunkter + avdoedKontrollpunkter + omsKontrollpunkter + fellesKontrollpunkter
            }

            Relasjon.SAMBOER -> {
                val (sak, avdoed) = hentDataForBeroert(hendelse)
                val samboer =
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = hendelse.beroertFnr,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = hendelse.sakTypeForEpsEllerBarn(),
                    )

                val samboerKontrollpunkt =
                    listOf(
                        DoedshendelseKontrollpunkt.SamboerSammeAdresseOgFellesBarn,
                    ) // todo: Liker ikke positiv sjekk
                val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                val omsKontrollpunkter = kontrollpunktOMSService.identifiser(hendelse, sak, samboer, avdoed)
                val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak) // todo: Trekk ut i egen

                samboerKontrollpunkt + avdoedKontrollpunkter + omsKontrollpunkter + fellesKontrollpunkter
            }

            Relasjon.AVDOED -> {
                kontrollerAvdoedHarYtelseIGjenny(hendelse)
            }
        }

    private fun hentDataForBeroert(hendelse: DoedshendelseInternal): Pair<Sak?, PersonDTO> {
        val sakType = hendelse.sakTypeForEpsEllerBarn()
        val sak = sakService.finnSak(hendelse.beroertFnr, sakType)
        val avdoed = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        return Pair(sak, avdoed)
    }

    private fun fellesKontrollpunkter(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> = listOfNotNull(kontrollerEksisterendeHendelser(hendelse, sak))

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
                    sakerForAvdoed.map {
                        kontrollerEksisterendeHendelser(hendelse, it)
                    }.first()
                listOfNotNull(DoedshendelseKontrollpunkt.AvdoedHarYtelse(sakerForAvdoed.first()), duplikat)
            } else {
                listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
            }
        }
    }

    private fun kontrollerEksisterendeHendelser(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
    ): DoedshendelseKontrollpunkt? {
        if (sak == null) {
            return null
        }

        val duplikatHendelse =
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId = sak.id,
                statuser = listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB),
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
