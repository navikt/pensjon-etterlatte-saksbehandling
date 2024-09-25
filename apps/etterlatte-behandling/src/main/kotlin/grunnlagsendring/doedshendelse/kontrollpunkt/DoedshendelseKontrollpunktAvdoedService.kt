package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.pdl.PersonDTO

internal class DoedshendelseKontrollpunktAvdoedService {
    fun identifiser(
        avdoed: PersonDTO,
        kontrollerDNummer: Boolean = true,
    ): List<DoedshendelseKontrollpunkt> {
        val kontrollpunkter = mutableListOf<DoedshendelseKontrollpunkt>()
        kontrollerDoedsdato(avdoed)?.let { kontrollpunkter.add(it) }
        if (kontrollerDNummer) kontrollerDNummer(avdoed)?.let { kontrollpunkter.add(it) }
        kontrollerUtvandring(avdoed)?.let { kontrollpunkter.add(it) }
        return kontrollpunkter
    }

    private fun kontrollerDoedsdato(avdoed: PersonDTO): DoedshendelseKontrollpunkt? =
        when (avdoed.doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
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
}
