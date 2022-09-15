package no.nav.etterlatte.brev.model

import java.time.LocalDate

data class Barn(val navn: String, val fnr: String)
data class Avdoed(val navn: String, val doedsdato: LocalDate)