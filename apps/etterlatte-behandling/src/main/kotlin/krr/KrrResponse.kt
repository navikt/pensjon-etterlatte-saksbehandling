package no.nav.etterlatte.krr

data class DigitalKontaktinformasjon(
    val personident: String,
    val aktiv: Boolean,
    val kanVarsles: Boolean?,
    val reservert: Boolean?, // Om man er reservert mot kommunikasjon på nett
    val spraak: String?,
    val epostadresse: String?,
    val mobiltelefonnummer: String?,
    val sikkerDigitalPostkasse: SikkerDigitalPostkasse?,
)

data class SikkerDigitalPostkasse(
    val adresse: String,
    val leverandoerAdresse: String,
    val leverandoerSertifikat: String,
)
