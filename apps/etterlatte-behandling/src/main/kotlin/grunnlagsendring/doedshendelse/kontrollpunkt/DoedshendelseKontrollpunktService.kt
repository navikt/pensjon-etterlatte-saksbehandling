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
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        val sakType = hendelse.sakType()
        val avdoed = pdlTjenesterKlient.hentPdlModell(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        val sak = sakService.finnSak(hendelse.beroertFnr, sakType)

        val kontrollPunkterForRelasjon =
            when (hendelse.relasjon) {
                Relasjon.BARN -> kontrollPunkterBarneRelasjon(hendelse, avdoed, sak)
                Relasjon.EPS -> {
                    val eps = pdlTjenesterKlient.hentPdlModell(hendelse.avdoedFnr, PersonRolle.GJENLEVENDE, sakType)
                    kontrollPunkterEpsRelasjon(hendelse, sak, eps, avdoed)
                }
            }
        return kontrollPunkterForRelasjon + fellesKontrollpunkter(hendelse, avdoed, sak)
    }

    fun fellesKontrollpunkter(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDTO,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerAvdoedDoedsdato(avdoed),
            kontrollerKryssendeYtelseEps(hendelse),
            kontrollerDNummer(avdoed),
            kontrollerUtvandring(avdoed),
            kontrollerEksisterendeHendelser(hendelse, sak),
        )
    }

    fun kontrollPunkterBarneRelasjon(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDTO,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerUfoeretrygdBarn(hendelse),
            kontrollerBarnOgHarBP(hendelse, sak),
            kontrollerSamtidigDoedsfall(avdoed, hendelse),
        )
    }

    fun kontrollPunkterEpsRelasjon(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerKryssendeYtelseEps(hendelse),
            kontrollerEksisterendeSakEps(hendelse, sak),
            kontrollerEpsErDoed(eps),
            kontrollerEpsHarFylt67Aar(eps),
            kontrollerSkiltSistefemAarEllerGiftI15(eps, avdoed),
            kontrollerSkilti5AarMedUkjentGiftemaalStart(eps, avdoed),
        )
    }

    private fun kontrollerSkilti5AarMedUkjentGiftemaalStart(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde? {
        if (eps.sivilstand != null) {
            val skilt = eps.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed = skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val naa = LocalDate.now()
            val gift = eps.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            if (skiltMedAvdoed.isNotEmpty() && giftMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = ChronoUnit.YEARS.between(naa, skiltsivilstand.gyldigFraOgMed)
                val giftSivilstand = giftMedAvdoed.first()
                return if (antallSkilteAar <= 5 && giftSivilstand.gyldigFraOgMed == null) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde
                } else {
                    null
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }

    private fun kontrollerSkiltSistefemAarEllerGiftI15(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5EllerGiftI15? {
        if (eps.sivilstand != null) {
            val skilt = eps.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed = skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val naa = LocalDate.now()
            if (skiltMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = ChronoUnit.YEARS.between(naa, skiltsivilstand.gyldigFraOgMed)
                return if (antallSkilteAar <= 5) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5EllerGiftI15
                } else {
                    null
                }
            }
            val gift = eps.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            if (giftMedAvdoed.isNotEmpty()) {
                val giftSivilstand = giftMedAvdoed.first()
                val antallGifteAar = ChronoUnit.YEARS.between(naa, giftSivilstand.gyldigFraOgMed)
                return if (antallGifteAar >= 15) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5EllerGiftI15
                } else {
                    null
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }

    private fun kontrollerEpsHarFylt67Aar(eps: PersonDTO): DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon? {
        return if (eps.foedselsdato != null) {
            val foedselsdato = eps.foedselsdato?.verdi
            val naaPlussEnMaaned = LocalDate.now().plusMonths(1L)
            val alder = ChronoUnit.YEARS.between(naaPlussEnMaaned, foedselsdato)
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

    private fun kontrollerEpsErDoed(eps: PersonDTO): DoedshendelseKontrollpunkt.EpsHarDoedsdato? {
        return if (eps.doedsdato != null) {
            DoedshendelseKontrollpunkt.EpsHarDoedsdato
        } else {
            null
        }
    }

    private fun kontrollerUfoeretrygdBarn(doedshendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt.BarnHarUfoereTrygd? {
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
        return this.sakType == UFORE_SAKTYPE &&
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

    private fun kontrollerKryssendeYtelseEps(hendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt? {
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
                true -> DoedshendelseKontrollpunkt.KryssendeYtelseIPesysEps
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

    private fun kontrollerEksisterendeSakEps(
        doedshendelse: DoedshendelseInternal,
        sak: Sak?,
    ): DoedshendelseKontrollpunkt? {
        if (doedshendelse.relasjon == Relasjon.EPS) {
            sak?.let { return DoedshendelseKontrollpunkt.EpsHarSakIGjenny(sak) }
        }
        return null
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
