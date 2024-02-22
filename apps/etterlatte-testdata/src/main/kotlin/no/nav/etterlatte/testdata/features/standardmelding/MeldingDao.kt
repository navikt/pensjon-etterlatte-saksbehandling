package no.nav.etterlatte.testdata.features.standardmelding

import java.time.LocalDateTime

fun payload(
    fnr: String,
    mottattDato: LocalDateTime,
) = """
    {
       "imageTag": "9f1f95b2472742227b37d19dd2d735ac9001995e",
       "spraak": "nb",
       "innsender": {
         "fornavn": {
           "svar": "GØYAL",
           "spoersmaal": "Fornavn"
         },
         "etternavn": {
           "svar": "HØYSTAKK",
           "spoersmaal": "Etternavn"
         },
         "foedselsnummer": {
           "svar": "01498344336",
           "spoersmaal": "Fødselsnummer"
         },
         "type": "INNSENDER"
       },
       "harSamtykket": {
         "svar": true,
         "spoersmaal": ""
       },
       "utbetalingsInformasjon": {
         "svar": {
           "verdi": "NORSK",
           "innhold": "Norsk"
         },
         "spoersmaal": "Ønsker du å motta utbetalingen på norsk eller utenlandsk bankkonto?",
         "opplysning": {
           "kontonummer": {
             "svar": {
               "innhold": "6848.64.44444"
             },
             "spoersmaal": "Oppgi norsk kontonummer for utbetaling"
           },
           "utenlandskBankNavn": null,
           "utenlandskBankAdresse": null,
           "iban": null,
           "swift": null,
           "skattetrekk": null
         }
       },
       "soeker": {
         "fornavn": {
           "svar": "kirsten",
           "spoersmaal": "Fornavn"
         },
         "etternavn": {
           "svar": "jakobsen",
           "spoersmaal": "Etternavn"
         },
         "foedselsnummer": {
           "svar": "25478323363",
           "spoersmaal": "Barnets fødselsnummer / d-nummer"
         },
         "statsborgerskap": {
           "svar": "Norge",
           "spoersmaal": "Statsborgerskap"
         },
         "utenlandsAdresse": {
           "svar": {
             "verdi": "NEI",
             "innhold": "Nei"
           },
           "spoersmaal": "Bor barnet i et annet land enn Norge?",
           "opplysning": null
         },
         "foreldre": [
           {
             "fornavn": {
               "svar": "GØYAL",
               "spoersmaal": "Fornavn"
             },
             "etternavn": {
               "svar": "HØYSTAKK",
               "spoersmaal": "Etternavn"
             },
             "foedselsnummer": {
               "svar": "25478323363",
               "spoersmaal": "Fødselsnummer"
             },
             "type": "FORELDER"
           },
           {
             "fornavn": {
               "svar": "fe",
               "spoersmaal": "Fornavn"
             },
             "etternavn": {
               "svar": "fe",
               "spoersmaal": "Etternavn"
             },
             "foedselsnummer": {
               "svar": "08498224343",
               "spoersmaal": "Fødselsnummer"
             },
             "type": "FORELDER"
           }
         ],
         "verge": {
           "svar": {
             "verdi": "NEI",
             "innhold": "Nei"
           },
           "spoersmaal": "Er det oppnevnt en verge for barnet?",
           "opplysning": null
         },
         "dagligOmsorg": null,
         "type": "BARN"
       },
       "foreldre": [
         {
           "fornavn": {
             "svar": "GØYAL",
             "spoersmaal": "Fornavn"
           },
           "etternavn": {
             "svar": "HØYSTAKK",
             "spoersmaal": "Etternavn"
           },
           "foedselsnummer": {
             "svar": "01498344336",
             "spoersmaal": "Fødselsnummer"
           },
           "adresse": {
             "svar": "Sannergata 6C, 0557 Oslo",
             "spoersmaal": "Bostedsadresse"
           },
           "statsborgerskap": {
             "svar": "Norge",
             "spoersmaal": "Statsborgerskap"
           },
           "kontaktinfo": {
             "telefonnummer": {
               "svar": {
                 "innhold": "11111111"
               },
               "spoersmaal": "Telefonnummer"
             }
           },
           "type": "GJENLEVENDE_FORELDER"
         },
         {
           "fornavn": {
             "svar": "fe",
             "spoersmaal": "Fornavn"
           },
           "etternavn": {
             "svar": "fe",
             "spoersmaal": "Etternavn"
           },
           "foedselsnummer": {
             "svar": "08498224343",
             "spoersmaal": "Fødselsnummer / d-nummer"
           },
           "datoForDoedsfallet": {
             "svar": {
               "innhold": "2022-01-01"
             },
             "spoersmaal": "Når skjedde dødsfallet?"
           },
           "statsborgerskap": {
             "svar": {
               "innhold": "Norge"
             },
             "spoersmaal": "Statsborgerskap"
           },
           "utenlandsopphold": {
             "svar": {
               "verdi": "NEI",
               "innhold": "Nei"
             },
             "spoersmaal": "Bodde eller arbeidet han eller hun i et annet land enn Norge etter fylte 16 år?",
             "opplysning": null
           },
           "doedsaarsakSkyldesYrkesskadeEllerYrkessykdom": {
             "svar": {
               "verdi": "NEI",
               "innhold": "Nei"
             },
             "spoersmaal": "Skyldes dødsfallet yrkesskade eller yrkessykdom?"
           },
           "naeringsInntekt": {
             "svar": {
               "verdi": "NEI",
               "innhold": "Nei"
             },
             "spoersmaal": "Var han eller hun selvstendig næringsdrivende?",
             "opplysning": null
           },
           "militaertjeneste": {
             "svar": {
               "verdi": "NEI",
               "innhold": "Nei"
             },
             "spoersmaal": "Har han eller hun gjennomført militær eller sivil førstegangstjeneste som varte minst 30 dager?",
             "opplysning": null
           },
           "type": "AVDOED"
         }
       ],
       "soesken": [],
       "versjon": "2",
       "type": "BARNEPENSJON",
       "mottattDato": "$mottattDato",
       "template": "barnepensjon_v2"
     }
    """.trimIndent()
