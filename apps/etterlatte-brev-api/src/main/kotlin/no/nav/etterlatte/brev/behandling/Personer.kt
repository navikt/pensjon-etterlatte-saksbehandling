package no.nav.etterlatte.brev.behandling

import java.time.LocalDate

data class Innsender(val navn: String, val fnr: String)

data class Soeker(val navn: String, val fnr: String)

data class Avdoed(val navn: String, val doedsdato: LocalDate)