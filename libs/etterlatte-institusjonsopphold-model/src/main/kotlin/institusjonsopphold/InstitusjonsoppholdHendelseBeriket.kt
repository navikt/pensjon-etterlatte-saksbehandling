package no.nav.etterlatte.institusjonsopphold

import java.time.LocalDate

data class InstitusjonsoppholdHendelseBeriket(
    val hendelseId: Long,
    val oppholdId: Long,
    val norskident: String,
    val institusjonsoppholdsType: InstitusjonsoppholdsType,
    val institusjonsoppholdKilde: InstitusjonsoppholdKilde,
    val institusjonsType: String? = null,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val institusjonsnavn: String? = null,
    val organisasjonsnummer: String? = null,
)

enum class InstitusjonsoppholdsType {
    INNMELDING,
    OPPDATERING,
    UTMELDING,
    ANNULERING,
}

enum class InstitusjonsoppholdKilde {
    APPBRK,
    KDI,
    IT,
    INST,
}
