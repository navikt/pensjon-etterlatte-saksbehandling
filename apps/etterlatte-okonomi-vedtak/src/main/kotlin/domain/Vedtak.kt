package no.nav.etterlatte.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Vedtak(
    val vedtakId: String,
    val behandlingsId: String,
    val sakId: String,
    val saksbehandlerId: String,
    val sakIdGjelderFnr: String,
    val aktorFoedselsdato: LocalDate,
    val behandlingstype: Endringskode,
    val beregningsperioder: List<Beregningsperiode>,
    val oppdragsenheter: List<Oppdragsenhet>,
)

data class Oppdragsenhet(
    val enhetsType: Enhetstype, // lengde: 4
    val enhetsnummer: String, // lengde: maks 13, men egentlig enhetsnummer 4 siffer
    val datoEnhetFOM: LocalDate
)

enum class Endringskode(s: String) {
    NY("NY"),
    ENDRING("ENDRING")
}

enum class Enhetstype(s: String) { //TODO finne ut forkortelse til Behandlende og evt. andre enhetstyper
    BOSTED("BOS")
}

enum class Ytelseskomponent(s: String) {
    BARNEPENSJON("BP")
}

data class Beregningsperiode(
    val delytelsesId: String,
    val ytelseskomponent: Ytelseskomponent,
    val datoFOM: LocalDate,
    val datoTOM: LocalDate,
    val belop: BigDecimal,
)
