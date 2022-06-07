package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

data class InntektsKomponentenResponse (
    val arbeidsInntektMaaned : List<ArbeidsInntektMaaned>?,
    val ident : Ident
)

data class Ident (
    val identifikator : String,
    val aktoerType : String
)

data class InntektListe (
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
    val beskrivelse : String,
    val skatteOgAvgiftsregel : String
)

data class ArbeidsInntektInformasjon (
    val inntektListe : List<InntektListe>
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

/*

{
"ident": {
    "identifikator": "10108000398",
    "aktoerType": "NATURLIG_IDENT"
  },
    "arbeidsInntektMaaned": [
    {
        "aarMaaned": "2016-01",
        "arbeidsInntektInformasjon": {
        "inntektListe": [
        {
            "inntektType": "LOENNSINNTEKT",
            "beloep": 25000,
            "fordel": "kontantytelse",
            "inntektskilde": "A-ordningen",
            "inntektsperiodetype": "Maaned",
            "inntektsstatus": "LoependeInnrapportert",
            "leveringstidspunkt": "2022-03",
            "utbetaltIMaaned": "2016-01",
            "opplysningspliktig": {
            "identifikator": "873152362",
            "aktoerType": "ORGANISASJON"
        },
        "virksomhet": {
            "identifikator": "873152362",
            "aktoerType": "ORGANISASJON"
        },
        "inntektsmottaker": {
            "identifikator": "10108000398",
            "aktoerType": "NATURLIG_IDENT"
        },
        "inngaarIGrunnlagForTrekk": true,
        "utloeserArbeidsgiveravgift": true,
        "informasjonsstatus": "InngaarAlltid",
        "beskrivelse": "fastloenn",
        "skatteOgAvgiftsregel": "nettoloenn"
        },
        ]

        }
    }

 */