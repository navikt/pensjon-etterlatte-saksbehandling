package testdata.features.soeknad

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.logger
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.testdata.JsonMessage
import java.time.OffsetDateTime
import java.util.UUID

object SoeknadMapper {
    fun opprettSoeknadJson(
        type: String,
        gjenlevendeFnr: String,
        avdoedFnr: String,
        barn: List<String> = emptyList(),
    ): String {
        val message =
            when (type) {
                "OMSTILLINGSSTOENAD" ->
                    opprettJsonMessageOmstilling(
                        soekerFnr = gjenlevendeFnr,
                        avdoedFnr = avdoedFnr,
                        barn = barn,
                    )

                "BARNEPENSJON" ->
                    opprettJsonMessageBarnepensjon(
                        gjenlevendeFnr,
                        avdoedFnr = avdoedFnr,
                        barnFnr = barn.first(),
                        soesken = barn.drop(1),
                    )

                else -> {
                    throw Exception("Ukjent soknad type: '$type'")
                }
            }

        return message.toJson().also {
            logger.info("Opprettet json: \n$it")
        }
    }

    private fun opprettJsonMessageBarnepensjon(
        gjenlevendeFnr: String,
        barnFnr: String,
        avdoedFnr: String,
        soesken: List<String>,
    ): JsonMessage {
        val mottattDato = Tidspunkt.now().toLocalDatetimeUTC()

        val skjemaInfo =
            """
            {
              "imageTag": "ce3542f9645d280bfff9936bdd0e7efc32424de2",
              "spraak": "nb",
              "innsender": ${mapInnsender(gjenlevendeFnr)},
              "harSamtykket": {"svar": true,"spoersmaal": ""},
              "utbetalingsInformasjon": ${mapUtbetalingsinfo()},
              "soeker": ${mapBarn(barnFnr, gjenlevendeFnr, avdoedFnr)},
              "foreldre": ${mapForeldre(gjenlevendeFnr, avdoedFnr)},
              "soesken": ${mapBarnListe(soesken, gjenlevendeFnr, avdoedFnr)},
              "versjon": "2",
              "type": "BARNEPENSJON",
              "mottattDato": "$mottattDato",
              "template": "barnepensjon_v2"
            }
            """.trimIndent()

        return JsonMessage.newMessage(
            mapOf(
                "@event_name" to SoeknadInnsendt.eventNameBehandlingBehov,
                "@skjema_info" to objectMapper.readValue<ObjectNode>(skjemaInfo),
                "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
                "@template" to "soeknad",
                "@fnr_soeker" to barnFnr,
                "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
                "@adressebeskyttelse" to "UGRADERT",
            ),
        )
    }

    private fun opprettJsonMessageOmstilling(
        soekerFnr: String,
        avdoedFnr: String,
        barn: List<String>,
    ): JsonMessage {
        val mottattDato = Tidspunkt.now().toLocalDatetimeUTC()

        val skjemaInfo =
            """
            {
              "imageTag": "ce3542f9645d280bfff9936bdd0e7efc32424de2",
              "spraak": "nb",
              "innsender": ${mapInnsender(soekerFnr)},
              "harSamtykket": {"svar": true,"spoersmaal": ""},
              "utbetalingsInformasjon": ${mapUtbetalingsinfo()},
              "soeker": ${mapGjenlevnde(soekerFnr)},
              "avdoed": ${mapAvdoed(avdoedFnr)},
              "barn": ${mapBarnListe(barn, soekerFnr, avdoedFnr)},
              "versjon": "1",
              "type": "OMSTILLINGSSTOENAD",
              "mottattDato": "$mottattDato",
              "template": "omstillingsstoenad"
            }
            """.trimIndent()

        return JsonMessage.newMessage(
            mapOf(
                "@event_name" to SoeknadInnsendt.eventNameInnsendt,
                "@skjema_info" to objectMapper.readValue<ObjectNode>(skjemaInfo),
                "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
                "@template" to "soeknad",
                "@fnr_soeker" to soekerFnr,
                "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
                "@adressebeskyttelse" to "UGRADERT",
            ),
        )
    }

    private fun mapUtbetalingsinfo(): String =
        """
         {
             "svar": {
               "verdi": "NORSK",
               "innhold": "Norsk"
             },
             "spoersmaal": "",
             "opplysning": {
               "kontonummer": {
                 "svar": {
                   "innhold": "1351.35.13513"
                 },
                 "spoersmaal": ""
               },
               "utenlandskBankNavn": null,
               "utenlandskBankAdresse": null,
               "iban": null,
               "swift": null,
               "skattetrekk": {
                 "svar": {
                   "verdi": "JA",
                   "innhold": "Ja"
                 },
                 "spoersmaal": "",
                 "opplysning": {
                   "svar": {
                     "innhold": "21%"
                   },
                   "spoersmaal": ""
                 }
               }
             }
        }
        """.trimIndent()

    private fun mapForeldre(
        gjenlevendeFnr: String,
        avdoedFnr: String,
    ) = """
        [
          ${mapGjenlevendeForelder(gjenlevendeFnr)},
          ${mapAvdoed(avdoedFnr)}
        ]
        """.trimIndent()

    private fun mapBarnListe(
        barnListe: List<String>,
        gjenlevendeFnr: String,
        avdoedFnr: String,
    ): String =
        barnListe.joinToString(separator = ",", prefix = "[", postfix = "]") {
            mapBarn(
                it,
                avdoedFnr,
                gjenlevendeFnr,
            )
        }

    private fun mapGjenlevnde(fnr: String) =
        """
        {
          "fornavn": {
            "svar": "LEVENDE",
            "spoersmaal": ""
          },
          "etternavn": {
            "svar": "TESTPERSON",
            "spoersmaal": ""
          },
          "foedselsnummer": {
            "svar": "$fnr",
            "spoersmaal": ""
          },
          "adresse": {
            "svar": "TESTVEIEN 123, 0123 TEST",
            "spoersmaal": ""
          },
          "sivilstatus": {
            "svar": "GIFT",
            "spoersmaal": ""
          },
          "nySivilstatus": {
            "svar": {
              "verdi": "ENSLIG",
              "innhold": "ENSLIG"
            },
            "spoersmaal": ""
          },
          "statsborgerskap": {
            "svar": "Norge",
            "spoersmaal": ""
          },
          "kontaktinfo": {
            "telefonnummer": {
              "svar": {
                "innhold": "11111111"
              },
              "spoersmaal": ""
            }
          },
          "andreYtelser": {
            "kravOmAnnenStonad": {
              "svar": {
                "verdi": "NEI",
                "innhold": "NEI"
              }
            },
            "annenPensjon": {
              "svar": {
                "verdi": "NEI",
                "innhold": "NEI"
              }
            },
            "pensjonUtland": {
              "svar": {
                "verdi": "NEI",
                "innhold": "NEI"
              }
            }
          },
          "uregistrertEllerVenterBarn": {
            "svar": {
              "verdi": "NEI",
              "innhold": "NEI"
            }       
          },
          "forholdTilAvdoede": {
            "relasjon": {
              "svar": {
                "verdi": "GIFT",
                "innhold": "GIFT"
              }
            }          
          },
          "type": "GJENLEVENDE"
        }
        """.trimIndent()

    private fun mapAvdoed(fnr: String) =
        """
        {
          "fornavn": {
            "svar": "DØD",
            "spoersmaal": ""
          },
          "etternavn": {
            "svar": "TESTPERSON",
            "spoersmaal": ""
          },
          "foedselsnummer": {
            "svar": "$fnr",
            "spoersmaal": ""
          },
          "datoForDoedsfallet": {
            "svar": {
              "innhold": "2021-07-27"
            },
            "spoersmaal": ""
          },
          "statsborgerskap": {
            "svar": {
              "innhold": "Norsk"
            },
            "spoersmaal": ""
          },
          "utenlandsopphold": {
            "svar": {
              "verdi": "NEI",
              "innhold": "Nei"
            },
            "spoersmaal": "",
            "opplysning": []
          },
          "doedsaarsakSkyldesYrkesskadeEllerYrkessykdom": {
            "svar": {
              "verdi": "NEI",
              "innhold": "Nei"
            },
            "spoersmaal": ""
          },
          "naeringsInntekt": {
            "svar": {
              "verdi": "JA",
              "innhold": "Ja"
            },
            "spoersmaal": "",
            "opplysning": {
              "naeringsinntektPrAarFoerDoedsfall": {
                "svar": {
                  "innhold": "150 000"
                },
                "spoersmaal": ""
              },
              "naeringsinntektVedDoedsfall": {
                "svar": {
                  "verdi": "NEI",
                  "innhold": "Nei"
                },
                "spoersmaal": ""
              }
            }
          },
          "militaertjeneste": {
            "svar": {
              "verdi": "JA",
              "innhold": "Ja"
            },
            "spoersmaal": "",
            "opplysning": {
              "svar": {
                "innhold": "1984"
              },
              "spoersmaal": ""
            }
          },
          "type": "AVDOED"
        }
        """.trimIndent()

    /*
     * typer
     *   "GJENLEVENDE_FORELDER"
     *   "FORELDER"
     */
    private fun mapGjenlevendeForelder(fnr: String) =
        """
        {
          "fornavn": {
            "svar": "Levende",
            "spoersmaal": ""
          },
          "etternavn": {
            "svar": "Testperson",
            "spoersmaal": ""
          },
          "foedselsnummer": {
            "svar": "$fnr",
            "spoersmaal": ""
          },
          "adresse": {
            "svar": "TESTVEIEN 123, 0123 TEST",
            "spoersmaal": ""
          },
          "statsborgerskap": {
            "svar": "Norge",
            "spoersmaal": ""
          },
          "kontaktinfo": {
            "telefonnummer": {
              "svar": {
                "innhold": "11111111"
              },
              "spoersmaal": ""
            }
          },
          "type": "GJENLEVENDE_FORELDER"
        }
        """.trimIndent()

    private fun mapForelder(fnr: String) =
        """
        {
          "fornavn": {
            "svar": "Levende",
            "spoersmaal": ""
          },
          "etternavn": {
            "svar": "Testperson",
            "spoersmaal": ""
          },
          "foedselsnummer": {
            "svar": "$fnr",
            "spoersmaal": ""
          },
          "type": "FORELDER"
        }
        """.trimIndent()

    private fun mapBarn(
        fnr: String,
        gjenlevende: String,
        avdoed: String,
    ) = """
        {
            "fornavn": {
              "svar": "TEST",
              "spoersmaal": ""
            },
            "etternavn": {
              "svar": "SOESKEN",
              "spoersmaal": ""
            },
            "foedselsnummer": {
              "svar": "$fnr",
              "spoersmaal": ""
            },
            "statsborgerskap": {
              "svar": "Norsk",
              "spoersmaal": ""
            },
            "utenlandsAdresse": {
              "svar": {
                "verdi": "NEI",
                "innhold": "Nei"
              },
              "spoersmaal": "",
              "opplysning": null
            },
            "foreldre": [
              ${mapForelder(gjenlevende)},
              ${mapForelder(avdoed)}
            ],
            "type": "BARN"
        }
        """.trimIndent()

    private fun mapInnsender(fnr: String) =
        """
        {
          "fornavn": {
            "svar": "DUMMY FORNAVN",
            "spoersmaal": ""
          },
          "etternavn": {
            "svar": "DUMMY ETTERNAVN",
            "spoersmaal": ""
          },
          "foedselsnummer": {
            "svar": "$fnr",
            "spoersmaal": ""
          },
          "type": "INNSENDER"
        }
        """.trimIndent()
}
