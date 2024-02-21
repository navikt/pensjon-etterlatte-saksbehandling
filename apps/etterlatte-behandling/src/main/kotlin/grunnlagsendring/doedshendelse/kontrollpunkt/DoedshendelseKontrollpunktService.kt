package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.ALDER_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.UFORE_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.LOPENDE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.OPPRETTET
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.TIL_BEHANDLING
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.slf4j.LoggerFactory

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val pesysKlient: PesysKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun identifiserKontrollerpunkter(hendelse: Doedshendelse): List<DoedshendelseKontrollpunkt> {
        val avdoed = pdlTjenesterKlient.hentPdlModell(hendelse.avdoedFnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        return listOfNotNull(
            kontrollerAvdoedDoedsdato(avdoed),
            kontrollerKryssendeYtelse(hendelse),
            kontrollerDNummer(avdoed),
            kontrollerUtvandring(avdoed),
            kontrollerSamtidigDoedsfall(avdoed, hendelse),
        )
    }

    private fun kontrollerAvdoedDoedsdato(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun kontrollerKryssendeYtelse(hendelse: Doedshendelse): DoedshendelseKontrollpunkt? =
        runBlocking {
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
        hendelse: Doedshendelse,
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
}
