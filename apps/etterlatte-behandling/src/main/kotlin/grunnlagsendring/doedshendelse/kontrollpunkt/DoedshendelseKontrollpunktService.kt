package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.ALDER_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.UFORE_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.LOPENDE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.OPPRETTET
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.TIL_BEHANDLING
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import kotlin.math.absoluteValue

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
    private val kontrollpunktBarnService =
        DoedshendelseKontrollpunktBarnService(
            pdlTjenesterKlient,
            pesysKlient,
            behandlingService,
        )

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
                val omsKontrollpunkter = kontrollpunkterOMS(hendelse, sak, eps, avdoed)
                val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak)

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

                val samboerKontrollpunkt = listOf(DoedshendelseKontrollpunkt.SamboerSammeAdresseOgFellesBarn)
                val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
                val omsKontrollpunkter = kontrollpunkterOMS(hendelse, sak, samboer, avdoed)
                val fellesKontrollpunkter = fellesKontrollpunkter(hendelse, sak)

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

    private fun kontrollpunkterOMS(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> =
        listOfNotNull(
            kontrollerKryssendeYtelseBeroert(hendelse),
            kontrollerEksisterendeSakBeroert(sak),
            kontrollerBeroertErDoed(eps),
            kontrollerBeroertFylt67Aar(eps, avdoed),
        )

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

    private fun kontrollerBeroertFylt67Aar(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon? {
        return if (eps.foedselsdato != null) {
            val foedselsdato = eps.foedselsdato?.verdi
            val maanedenEtterDoedsdato = avdoed.doedsdato!!.verdi.plusMonths(1).withDayOfMonth(1)
            val alder = safeYearsBetween(foedselsdato, maanedenEtterDoedsdato).absoluteValue
            val alderspensjonAar = 67
            if (alder >= alderspensjonAar) {
                DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun kontrollerBeroertErDoed(eps: PersonDTO): DoedshendelseKontrollpunkt.EpsHarDoedsdato? {
        return if (eps.doedsdato != null) {
            DoedshendelseKontrollpunkt.EpsHarDoedsdato
        } else {
            null
        }
    }

    private fun kontrollerKryssendeYtelseBeroert(hendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt? {
        if (hendelse.sakTypeForEpsEllerBarn() == SakType.BARNEPENSJON) {
            return null
        }
        return runBlocking {
            val kryssendeYtelser =
                pesysKlient.hentSaker(hendelse.beroertFnr)
                    .filter { it.sakStatus in listOf(OPPRETTET, TIL_BEHANDLING, LOPENDE) }
                    .filter { it.sakType in listOf(UFORE_SAKTYPE, ALDER_SAKTYPE) }
                    .filter { it.fomDato == null || it.fomDato.isBefore(hendelse.avdoedDoedsdato) }
                    .any { it.tomDate == null || it.tomDate.isAfter(hendelse.avdoedDoedsdato) }

            when (kryssendeYtelser) {
                true -> DoedshendelseKontrollpunkt.KryssendeYtelseIPesysEps
                false -> null
            }
        }
    }

    private fun kontrollerEksisterendeSakBeroert(sak: Sak?): DoedshendelseKontrollpunkt? {
        return sak?.let {
            val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(it.id)
            return when (sisteIverksatteBehandling) {
                is Behandling ->
                    DoedshendelseKontrollpunkt.EpsHarSakMedIverksattBehandlingIGjenny(
                        sak,
                    )

                null -> null
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
