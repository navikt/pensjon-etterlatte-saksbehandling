package no.nav.etterlatte.institusjonsopphold.model

import java.time.LocalDate
import java.time.LocalDateTime

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

data class InstitusjonsoppholdForPersoner(
    val data: Map<String, List<Institusjonsopphold>>,
)

data class Institusjonsopphold(
    val oppholdId: Long,
    val institusjonsnavn: String? = null,
    val avdelingsnavn: String? = null,
    val organisasjonsnummer: String? = null,
    val institusjonstype: String? = null,
    val kategori: String? = null,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val endringstidspunkt: LocalDateTime? = null,
)
