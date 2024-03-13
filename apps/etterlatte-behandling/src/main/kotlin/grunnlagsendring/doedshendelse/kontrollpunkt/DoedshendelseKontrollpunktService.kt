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
import java.time.temporal.Temporal
import kotlin.math.absoluteValue

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
        val kontrollpunkterForRelasjon =
            when (hendelse.relasjon) {
                Relasjon.BARN -> {
                    val (sak, avdoed) = hentDataForBeroert(hendelse)
                    kontrollpunkterBarneRelasjon(hendelse, avdoed, sak) + fellesKontrollpunkter(hendelse, avdoed, sak)
                }

                Relasjon.EKTEFELLE -> {
                    val (sak, avdoed) = hentDataForBeroert(hendelse)
                    val eps =
                        pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                            foedselsnummer = hendelse.beroertFnr,
                            rolle = PersonRolle.GJENLEVENDE,
                            saktype = hendelse.sakTypeForEpsEllerBarn(),
                        )
                    kontrollpunkterEpsRelasjon(hendelse, sak, eps, avdoed) +
                        fellesKontrollpunkter(
                            hendelse = hendelse,
                            avdoed = avdoed,
                            sak = sak,
                        )
                }

                Relasjon.AVDOED -> {
                    kontrollerAvdoedHarYtelseIGjenny(hendelse)
                }

                Relasjon.SAMBOER -> {
                    val (sak, avdoed) = hentDataForBeroert(hendelse)
                    val samboer =
                        pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                            foedselsnummer = hendelse.beroertFnr,
                            rolle = PersonRolle.GJENLEVENDE,
                            saktype = hendelse.sakTypeForEpsEllerBarn(),
                        )

                    kontrollpunkterEpsRelasjon(hendelse, sak, samboer, avdoed) +
                        fellesKontrollpunkter(
                            hendelse = hendelse,
                            avdoed = avdoed,
                            sak = sak,
                        ) + listOf(DoedshendelseKontrollpunkt.SamboerSammeAdresseOgFellesBarn)
                }
            }

        return kontrollpunkterForRelasjon
    }

    private fun hentDataForBeroert(hendelse: DoedshendelseInternal): Pair<Sak?, PersonDTO> {
        val sakType = hendelse.sakTypeForEpsEllerBarn()
        val sak = sakService.finnSak(hendelse.beroertFnr, sakType)
        val avdoed = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        return Pair(sak, avdoed)
    }

    private fun fellesKontrollpunkter(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDTO,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerAvdoedDoedsdato(avdoed),
            kontrollerDNummer(avdoed),
            kontrollerUtvandring(avdoed),
            kontrollerEksisterendeHendelser(hendelse, sak),
        )
    }

    private fun kontrollpunkterBarneRelasjon(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDTO,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerUfoeretrygdBarn(hendelse),
            kontrollerBarnOgHarBP(sak),
            kontrollerSamtidigDoedsfall(avdoed, hendelse),
        )
    }

    private fun kontrollpunkterEpsRelasjon(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerKryssendeYtelseEps(hendelse),
            kontrollerEksisterendeSakEps(sak),
            kontrollerEpsErDoed(eps),
            kontrollerEpsHarFylt67Aar(eps),
            kontrollerSkiltSistefemAarEllerGiftI15(eps, avdoed),
            kontrollerSkilti5AarMedUkjentGiftemaalStart(eps, avdoed),
        )
    }

    private fun kontrollerAvdoedHarYtelseIGjenny(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> {
        val sakerForAvdoed = sakService.finnSaker(hendelse.avdoedFnr)
        return if (sakerForAvdoed.isEmpty()) {
            listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
        } else {
            val duplikat =
                sakerForAvdoed.map {
                    kontrollerEksisterendeHendelser(hendelse, it)
                }.first()
            listOfNotNull(DoedshendelseKontrollpunkt.AvdoedHarYtelse(sakerForAvdoed.first()), duplikat)
        }
    }

    private fun kontrollerSkilti5AarMedUkjentGiftemaalStart(
        ektefelle: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde? {
        if (ektefelle.sivilstand != null) {
            val skilt = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed =
                skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val naa = LocalDate.now()
            val gift = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            if (skiltMedAvdoed.isNotEmpty() && giftMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = safeYearsBetween(skiltsivilstand.gyldigFraOgMed, naa).absoluteValue
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
        ektefelle: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5OgGiftI15? {
        if (ektefelle.sivilstand != null) {
            val skilt = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed =
                skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val gift = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val naa = LocalDate.now()
            if (skiltMedAvdoed.isNotEmpty() && giftMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = safeYearsBetween(naa, skiltsivilstand.gyldigFraOgMed).absoluteValue
                val giftSivilstand = giftMedAvdoed.first()
                val antallGifteAar = safeYearsBetween(giftSivilstand.gyldigFraOgMed, naa).absoluteValue
                return if (antallSkilteAar <= 5 && antallGifteAar >= 15) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5OgGiftI15
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
            val alder = safeYearsBetween(foedselsdato, naaPlussEnMaaned).absoluteValue
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

    private fun safeYearsBetween(
        first: Temporal?,
        second: Temporal?,
    ): Long {
        if (first == null) {
            return 0L
        }
        if (second == null) {
            return 0L
        }

        return ChronoUnit.YEARS.between(first, second).absoluteValue
    }

    private fun kontrollerEpsErDoed(eps: PersonDTO): DoedshendelseKontrollpunkt.EpsHarDoedsdato? {
        return if (eps.doedsdato != null) {
            DoedshendelseKontrollpunkt.EpsHarDoedsdato
        } else {
            null
        }
    }

    private fun kontrollerUfoeretrygdBarn(doedshendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt.BarnHarUfoereTrygd? {
        return when (harUfoereTrygd(doedshendelse)) {
            true -> DoedshendelseKontrollpunkt.BarnHarUfoereTrygd
            false -> null
        }
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

    private fun kontrollerBarnOgHarBP(sak: Sak?): DoedshendelseKontrollpunkt.BarnHarBarnepensjon? {
        if (sak == null) {
            return null
        }

        val hentSisteIverksatte = behandlingService.hentSisteIverksatte(sak.id)

        val harBarnepensjon = hentSisteIverksatte != null
        return if (harBarnepensjon) {
            DoedshendelseKontrollpunkt.BarnHarBarnepensjon(sak)
        } else {
            null
        }
    }

    private fun kontrollerAvdoedDoedsdato(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun kontrollerKryssendeYtelseEps(hendelse: DoedshendelseInternal): DoedshendelseKontrollpunkt? {
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
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = hendelse.beroertFnr,
                        rolle = PersonRolle.BARN,
                        saktype = SakType.BARNEPENSJON,
                    ).familieRelasjon?.verdi?.foreldre
                        ?.map { it.value }
                        ?.firstOrNull { it != hendelse.avdoedFnr }

                if (annenForelderFnr == null) {
                    DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
                } else {
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = annenForelderFnr,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = SakType.BARNEPENSJON,
                    )
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

    private fun kontrollerEksisterendeSakEps(sak: Sak?): DoedshendelseKontrollpunkt? =
        sak?.let { return DoedshendelseKontrollpunkt.EpsHarSakIGjenny(sak) }

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
