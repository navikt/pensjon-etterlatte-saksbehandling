package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

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
    val oensketSkattetrekkProsent: String?
)