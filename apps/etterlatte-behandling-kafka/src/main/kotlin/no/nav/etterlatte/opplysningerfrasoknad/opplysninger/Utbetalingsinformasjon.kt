package no.nav.etterlatte.opplysningerfrasoknad.opplysninger

import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType

data class Utbetalingsinformasjon(
    val bankkontoType: BankkontoType?,
    val kontonummer: String?,
    val utenlandskBankNavn: String?,
    val utenlandskBankAdresse: String?,
    val iban: String?,
    val swift: String?,
)
