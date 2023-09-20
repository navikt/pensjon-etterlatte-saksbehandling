package no.nav.etterlatte.libs.common.trygdetid.avtale

import java.time.LocalDate

data class TrygdetidAvtale(
    val kode: String,
    val beskrivelse: String,
    val fraDato: LocalDate,
    val datoer: List<TrygdetidAvtaleDato>,
)

data class TrygdetidAvtaleDato(
    val kode: String,
    val beskrivelse: String,
    val fraDato: LocalDate,
)

data class TrygdetidAvtaleKriteria(
    val kode: String,
    val beskrivelse: String,
    val fraDato: LocalDate,
)
