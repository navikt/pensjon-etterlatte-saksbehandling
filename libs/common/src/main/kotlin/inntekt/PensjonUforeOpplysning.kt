package no.nav.etterlatte.libs.common.inntekt

data class PensjonUforeOpplysning(
    val mottattUforetrygd: List<Inntekt>,
    val mottattAlderspensjon: List<Inntekt>
)


data class Ident (
    val identifikator : String,
    val aktoerType : String
)

data class Inntekt (
    val inntektType : String,
    val beloep : Int,
    val fordel : String,
    val inntektskilde : String,
    val inntektsperiodetype : String,
    val inntektsstatus : String,
    val leveringstidspunkt : String,
    val utbetaltIMaaned : String,
    val opplysningspliktig : Opplysningspliktig,
    val virksomhet : Virksomhet,
    val inntektsmottaker : Inntektsmottaker,
    val inngaarIGrunnlagForTrekk : Boolean,
    val utloeserArbeidsgiveravgift : Boolean,
    val informasjonsstatus : String,
    val beskrivelse : InntektsBeskrivelse,
    val skatteOgAvgiftsregel : String
)

enum class InntektsBeskrivelse(val value: String) {
    UFORETRYGD("ufoeretrygd"),
    ALDERSPENSJON("alderspensjon")
}

data class ArbeidsInntektInformasjon (
    val inntektListe : List<Inntekt>?
)

data class Inntektsmottaker (
    val identifikator : Int,
    val aktoerType : String
)

data class Opplysningspliktig (
    val identifikator : Int,
    val aktoerType : String
)

data class Virksomhet (
    val identifikator : Int,
    val aktoerType : String
)

data class ArbeidsInntektMaaned (
    val aarMaaned : String,
    val arbeidsInntektInformasjon : ArbeidsInntektInformasjon
)