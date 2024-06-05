package no.nav.etterlatte.opplysningerfrasoknad.opplysninger

import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke

data class Utbetalingsinformasjon(
    val bankkontoType: BankkontoType?,
    val kontonummer: String?,
    val utenlandskBankNavn: String?,
    val utenlandskBankAdresse: String?,
    val iban: String?,
    val swift: String?,
    val oenskerSkattetrekk: JaNeiVetIkke?,
    val oensketSkattetrekk: String?,
    val beskrivelse: String?,
)
