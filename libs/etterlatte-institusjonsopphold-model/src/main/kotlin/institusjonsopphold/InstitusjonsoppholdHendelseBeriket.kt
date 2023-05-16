package institusjonsopphold

import java.time.LocalDate

data class InstitusjonsoppholdHendelseBeriket(
    val hendelseId: Long,
    val oppholdId: Long,
    val norskident: String,
    val type: Type,
    val kilde: Kilde,
    val institusjonsType: String,
    val startdato: LocalDate? = null,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val institusjonsnavn: String? = null
)

enum class Type {
    INNMELDING,
    OPPDATERING,
    UTMELDING,
    ANNULERING
}

enum class Kilde {
    APPBRK, KDI, IT, INST
}