package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto

internal class DoedshendelseKontrollpunktAvdoedService {
    fun identifiser(avdoed: PersonDoedshendelseDto): List<DoedshendelseKontrollpunkt> =
        listOfNotNull(
            kontrollerDoedsdato(avdoed),
            kontrollerDNummer(avdoed),
            kontrollerUtvandring(avdoed),
        )

    private fun kontrollerDoedsdato(avdoed: PersonDoedshendelseDto): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun kontrollerDNummer(avdoed: PersonDoedshendelseDto): DoedshendelseKontrollpunkt? =
        when (avdoed.foedselsnummer.verdi.isDNumber()) {
            true -> DoedshendelseKontrollpunkt.AvdoedHarDNummer
            false -> null
        }

    private fun kontrollerUtvandring(avdoed: PersonDoedshendelseDto): DoedshendelseKontrollpunkt? {
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
}
