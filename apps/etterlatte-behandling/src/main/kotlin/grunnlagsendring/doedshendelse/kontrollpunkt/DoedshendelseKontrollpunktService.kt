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

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val pesysKlient: PesysKlient,
) {
    fun identifiserKontrollerpunkter(hendelse: Doedshendelse): List<DoedshendelseKontrollpunkt> {
        val kontrollpunkter = mutableListOf<DoedshendelseKontrollpunkt>()
        val avdoed = pdlTjenesterKlient.hentPdlModell(hendelse.avdoedFnr, PersonRolle.AVDOED, SakType.BARNEPENSJON)

        kontrollpunkter.addNotNull(kontrollerAvdoedDoedsdato(avdoed))
        kontrollpunkter.addNotNull(kontrollerKryssendeYtelse(hendelse))
        kontrollpunkter.addNotNull(kontrollerDNummer(avdoed))
        kontrollpunkter.addNotNull(kontrollerUtvandring(avdoed))

        return kontrollpunkter
    }

    private fun kontrollerAvdoedDoedsdato(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun kontrollerKryssendeYtelse(hendelse: Doedshendelse): DoedshendelseKontrollpunkt? =
        runBlocking {
            val kryssendeYtelser =
                pesysKlient.hentSaker(hendelse.avdoedFnr)
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

        if (utflytting != null) {
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
}

private fun MutableList<DoedshendelseKontrollpunkt>.addNotNull(element: DoedshendelseKontrollpunkt?) {
    element?.let { add(it) }
}
