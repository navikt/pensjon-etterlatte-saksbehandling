package no.nav.etterlatte.testdata.features.soeknad // ktlint-disable filename

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.producer
import no.nav.etterlatte.testdata.JsonMessage
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

object OpprettSoeknadFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad manuelt"
    override val path: String
        get() = "soeknad"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "soeknad/ny-soeknad.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path
                        )
                    )
                )
            }

            post {
                try {
                    val (partisjon, offset) = call.receiveParameters().let {
                        producer.publiser(
                            requireNotNull(it["key"]),
                            opprettSoeknadJson(
                                gjenlevendeFnr = it["fnrGjenlevende"]!!,
                                avdoedFnr = it["fnrAvdoed"]!!,
                                barnFnr = it["fnrBarn"]!!
                            ),
                            mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                        )
                    }
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    call.respondRedirect("/$path/sendt?partisjon=$partisjon&offset=$offset")
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString())
                        )
                    )
                }
            }

            get("sendt") {
                val partisjon = call.request.queryParameters["partisjon"]!!
                val offset = call.request.queryParameters["offset"]!!

                call.respond(
                    MustacheContent(
                        "soeknad/soeknad-sendt.hbs",
                        mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "partisjon" to partisjon,
                            "offset" to offset
                        )
                    )
                )
            }
        }
}

private fun opprettSoeknadJson(gjenlevendeFnr: String, avdoedFnr: String, barnFnr: String): String {
    val skjemaInfo = opprettSkjemaInfo(gjenlevendeFnr, barnFnr, avdoedFnr, LocalDateTime.now())

    return JsonMessage.newMessage(
        mapOf(
            "@event_name" to "soeknad_innsendt",
            "@skjema_info" to objectMapper.readValue<ObjectNode>(skjemaInfo),
            "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
            "@template" to "soeknad",
            "@fnr_soeker" to barnFnr,
            "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
            "@adressebeskyttelse" to "UGRADERT"
        )
    ).toJson()
}

private fun opprettSkjemaInfo(
    gjenlevendeFnr: String,
    barnFnr: String,
    avdoedFnr: String,
    mottattDato: LocalDateTime
) = """
    {
      "imageTag": "ce3542f9645d280bfff9936bdd0e7efc32424de2",
      "spraak": "nb",
      "innsender": {
        "fornavn": {
          "svar": "DUMMY FORNAVN",
          "spoersmaal": ""
        },
        "etternavn": {
          "svar": "DUMMY ETTERNAVN",
          "spoersmaal": ""
        },
        "foedselsnummer": {
          "svar": "$gjenlevendeFnr",
          "spoersmaal": ""
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
      },
      "soeker": {
        "fornavn": {
          "svar": "TEST",
          "spoersmaal": ""
        },
        "etternavn": {
          "svar": "SOEKER",
          "spoersmaal": ""
        },
        "foedselsnummer": {
          "svar": "$barnFnr",
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
              "svar": "$gjenlevendeFnr",
              "spoersmaal": ""
            },
            "type": "FORELDER"
          },
          {
            "fornavn": {
              "svar": "Død",
              "spoersmaal": ""
            },
            "etternavn": {
              "svar": "Testperson",
              "spoersmaal": ""
            },
            "foedselsnummer": {
              "svar": "$avdoedFnr",
              "spoersmaal": ""
            },
            "type": "FORELDER"
          }
        ],
        "verge": {
          "svar": {
            "verdi": "NEI",
            "innhold": "Nei"
          },
          "spoersmaal": "",
          "opplysning": null
        },
        "dagligOmsorg": null,
        "type": "BARN"
      },
      "foreldre": [
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
            "svar": "$gjenlevendeFnr",
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
        },
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
            "svar": "$avdoedFnr",
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
      ],
      "soesken": [],
      "versjon": "2",
      "type": "BARNEPENSJON",
      "mottattDato": "$mottattDato",
      "template": "barnepensjon_v2"
    }
""".trimIndent()