package no.nav.etterlatte.libs.common.skjermet

import no.nav.etterlatte.libs.common.dbutils.Tidspunkt

data class EgenAnsattSkjermet(
    val fnr: String,
    val inntruffet: Tidspunkt,
    val skjermet: Boolean,
)
