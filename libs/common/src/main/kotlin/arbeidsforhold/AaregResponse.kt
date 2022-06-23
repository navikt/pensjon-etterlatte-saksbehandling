package no.nav.etterlatte.libs.common.arbeidsforhold

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class AaregResponse(
    val id: Int,
    val type: AaregKodeBeskrivelse,
    val arbeidstaker: AaregArbeidstaker,
    val arbeidssted: AaregArbeidssted,
    val opplysningsPliktig: AaregOpplysningspliktig,
    val ansettelsesperiode: AaregAnsettelsesperiode,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>,
    val rapporteringsordning: List<AaregKodeBeskrivelse>,
    val navArbeidsforholdId: Int,
    val navVersjon: Int,
    val navUuid: String,
    val opprettet: LocalDateTime,
    val sistBekreftet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val bruksperiode: AaregBruksperiode
)


data class AaregArbeidstaker(
    val identer: List<AaregIdent>
)
data class AaregIdent(
    val type: String,
    val ident: String,
    val gjeldende: Boolean
)
data class AaregArbeidssted(
    val type: String,
    val identer: List<AaregIdent>
)
data class AaregOpplysningspliktig(
    val type: String,
    val identer: List<AaregIdent>
)
data class AaregAnsettelsesperiode(
    val startdato: LocalDate,
)
data class AaregAnsettelsesdetaljer(
    val type: String,
    val arbeidstidsordning: AaregKodeBeskrivelse,
    val yrke: AaregKodeBeskrivelse,
    val antallTimerPrUke: Double,
    val avtaltStillingsprosent: Double,
    val rapporteringsmaaneder: AaregFraTil
)
data class AaregKodeBeskrivelse(
    val kode: String,
    val beskrivelse: String
)
data class AaregFraTil(
    val fra: YearMonth,
    val til: YearMonth
)
data class AaregBruksperiode(
    val fom: LocalDateTime,
    val tom: LocalDateTime
)