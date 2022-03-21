package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Utbetalingsinformasjon(
    val bankkontoType: BankkontoType?,
    val kontonummer: String?,
    val utenlandskBankNavn: String?,
    val utenlandskBankAdresse: String?,
    val iban: String?,
    val swift: String?,
    val oenskerSkattetrekk: JaNeiVetIkke?,
    val oensketSkattetrekkProsent: String?,
)