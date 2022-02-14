package no.nav.etterlatte.batch

fun payload(fnr: String) = """
{
  "@event_name": "soeknad_innsendt",
  "@skjema_info": {
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
        "svar": "24107321582",
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
        "svar": "07081177656",
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
            "svar": "24107321582",
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
            "svar": "09116224442",
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
          "svar": "24107321582",
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
          "svar": "09116224442",
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
    "mottattDato": "2022-02-14T14:37:24.573612786",
    "template": "barnepensjon_v2"
  },
  "@lagret_soeknad_id": 900,
  "@template": "soeknad",
  "@fnr_soeker": "07081177656",
  "@hendelse_gyldig_til": "2023-02-14T14:09:20.684508951Z",
  "system_read_count": 2,
  "system_participating_services": [
    {
      "service": "innsendt-soeknad",
      "instance": "innsendt-soeknad-57b4b88446-bh5b5",
      "time": "2022-02-14T14:39:20.684818363"
    },
    {
      "service": "sjekk-adressebeskyttelse",
      "instance": "sjekk-adressebeskyttelse-6cf4bd7b5d-8sqdh",
      "time": "2022-02-14T14:39:20.690640305"
    },
    {
      "service": "journalfoer-soeknad",
      "instance": "journalfoer-soeknad-76c4c68cfb-k57r8",
      "time": "2022-02-14T14:39:21.044308399"
    }
  ],
  "@adressebeskyttelse": "UGRADERT",
  "@dokarkivRetur": {
    "journalpostId": "524975071",
    "journalpoststatus": null,
    "melding": null,
    "journalpostferdigstilt": false,
    "dokumenter": [
      {
        "dokumentInfoId": "549167948"
      }
    ]
  }
}
""".trimIndent()
