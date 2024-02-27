package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
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

    fun identifiserKontrollerpunkter(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> {
        val avdoed = pdlTjenesterKlient.hentPdlModell(hendelse.avdoedFnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)
        val sak = sakService.finnSak(hendelse.beroertFnr, hendelse.sakType())

        return listOfNotNull(
            kontrollerUfoeretrygdBarn(hendelse),
            kontrollerBarnOgHarBP(hendelse, sak),
            kontrollerAvdoedDoedsdato(avdoed),
            kontrollerKryssendeYtelse(hendelse),
            kontrollerDNummer(avdoed),
            kontrollerUtvandring(avdoed),
            kontrollerSamtidigDoedsfall(avdoed, hendelse),
            kontrollerEksisterendeSak(sak),
            kontrollerEksisterendeHendelser(hendelse, sak),
        )
    }

    fun kontrollerUfoeretrygdBarn(doedshendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt.BarnHarUfoereTrygd? {
        if (doedshendelse.relasjon == Relasjon.BARN) {
            return when (harUfoereTrygd(doedshendelse)) {
                true -> DoedshendelseKontrollpunkt.BarnHarUfoereTrygd
                false -> null
            }
        }
        return null
    }

    private fun harUfoereTrygd(doedshendelse: DoedshendelseInternal): Boolean {
        val sakerFraPesys =
            runBlocking {
                pesysKlient.hentSaker(doedshendelse.beroertFnr)
            }
        return sakerFraPesys.any { it.harLoependeUfoeretrygd() }
    }

    private fun SakSammendragResponse.harLoependeUfoeretrygd(): Boolean {
        return this.sakType == SakSammendragResponse.UFORE_SAKTYPE &&
            this.sakStatus == SakSammendragResponse.Status.LOPENDE
    }

    fun kontrollerBarnOgHarBP(
        doedshendelse: DoedshendelseInternal,
        sak: Sak?,
    ): DoedshendelseKontrollpunkt.BarnHarBarnepensjon? {
        if (doedshendelse.relasjon == Relasjon.BARN) {
            if (sak == null) {
                return null
            }
            val hentSisteIverksatte = behandlingService.hentSisteIverksatte(sak.id)
            val harIkkeBarnepensjon = hentSisteIverksatte == null
            if (harIkkeBarnepensjon) {
                return null
            } else {
                return DoedshendelseKontrollpunkt.BarnHarBarnepensjon
            }
        }
        return null
    }

    private fun kontrollerAvdoedDoedsdato(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun kontrollerKryssendeYtelse(hendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt? {
        if (hendelse.sakType() == SakType.BARNEPENSJON) {
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
                true -> DoedshendelseKontrollpunkt.KryssendeYtelseIPesys
                false -> null
            }
        }
    }

    private fun kontrollerDNummer(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.foedselsnummer.verdi.isDNumber()) {
            true -> DoedshendelseKontrollpunkt.AvdoedHarDNummer
            false -> null
        }

    private fun kontrollerUtvandring(avdoed: PersonDTO): DoedshendelseKontrollpunkt? {
        val utflytting = avdoed.utland?.verdi?.utflyttingFraNorge
        val innflytting = avdoed.utland?.verdi?.innflyttingTilNorge

        if (!utflytting.isNullOrEmpty()) {
            if (utflytting.any { it.dato == null }) {
                // Hvis vi ikke har noen dato så regner vi personen som utvandret på nåværende tidspunkt uansett
                return DoedshendelseKontrollpunkt.AvdoedHarUtvandret
            }

            val sisteRegistrerteUtflytting = utflytting.mapNotNull { it.dato }.max()
            val sisteRegistrerteInnflytting = innflytting?.mapNotNull { it.dato }?.maxOrNull()

            if (sisteRegistrerteInnflytting != null && sisteRegistrerteInnflytting.isAfter(sisteRegistrerteUtflytting)) {
                return null
            }

            return DoedshendelseKontrollpunkt.AvdoedHarUtvandret
        }

        return null
    }

    private fun kontrollerSamtidigDoedsfall(
        avdoed: PersonDTO,
        hendelse: DoedshendelseInternal,
    ): DoedshendelseKontrollpunkt? =
        try {
            avdoed.doedsdato?.verdi?.let { avdoedDoedsdato ->
                val annenForelderFnr =
                    pdlTjenesterKlient.hentPdlModell(
                        foedselsnummer = hendelse.beroertFnr,
                        rolle = PersonRolle.BARN,
                        saktype = SakType.BARNEPENSJON,
                    ).familieRelasjon?.verdi?.foreldre
                        ?.map { it.value }
                        ?.firstOrNull { it != hendelse.avdoedFnr }

                if (annenForelderFnr == null) {
                    DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
                } else {
                    pdlTjenesterKlient.hentPdlModell(annenForelderFnr, PersonRolle.GJENLEVENDE, SakType.BARNEPENSJON)
                        .doedsdato?.verdi?.let { annenForelderDoedsdato ->
                            if (annenForelderDoedsdato == avdoedDoedsdato) {
                                DoedshendelseKontrollpunkt.SamtidigDoedsfall
                            } else {
                                null
                            }
                        }
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av annen forelder", e)
            DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
        }

    private fun kontrollerEksisterendeSak(sak: Sak?): DoedshendelseKontrollpunkt? =
        sak?.let { return DoedshendelseKontrollpunkt.SakEksistererIGjenny(sak) }

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
