package no.nav.etterlatte.libs.common.vedtak

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
    val attestasjon: Attestasjon
)

data class Oppdragsenhet(
    val enhetsType: Enhetstype, // lengde: 4
    val enhetsnummer: String, // lengde: maks 13, men egentlig enhetsnummer 4 siffer
    val datoEnhetFOM: LocalDate
)

enum class Endringskode() {
    NY,
    ENDRING
}

enum class Enhetstype(s: String) { // TODO finne ut forkortelse til Behandlende og evt. andre enhetstyper
    BOSTED("BOS")
}

data class Beregningsperiode(
    val delytelsesId: String,
    val ytelseskomponent: String,
    val datoFOM: LocalDate,
    val datoTOM: LocalDate,
    val belop: BigDecimal
)