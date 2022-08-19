package testdata.features.dolly


import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import dolly.DollyService
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.getClientAccessToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.objectMapper
import no.nav.etterlatte.producer
import no.nav.etterlatte.usernameFraToken
import java.time.OffsetDateTime
import java.util.*

class DollyFeature(private val dollyService: DollyService) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad automatisk via Dolly"
    override val path: String
        get() = "dolly"

    override val routes: Route.() -> Unit
        get() = {
            get {
                val accessToken = getClientAccessToken()
                val gruppeId = dollyService.hentTestGruppe(usernameFraToken()!!, accessToken)

                call.respond(
                    MustacheContent(
                        "dolly/dolly.hbs", mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId
                        )
                    )
                )
            }

            get("hent-familier") {
                try {
                    val accessToken = getClientAccessToken()
                    val gruppeId = call.request.queryParameters["gruppeId"]!!.toLong()

                    val familier = try {
                        dollyService.hentFamilier(gruppeId, accessToken)
                    } catch (ex: Exception) {
                        logger.error("Klarte ikke hente familier", ex)
                        emptyList()
                    }

                    call.respond(familier.toJson())
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

            get("opprett-familie") {
                try {
                    val accessToken = getClientAccessToken()
                    dollyService.hentTestGruppe(usernameFraToken()!!, accessToken)?.let { id ->
                        dollyService.opprettBestilling(bestilling, id, accessToken).also { bestilling ->
                            logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
                            call.respond(bestilling.toJson())
                        }
                    }
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

            post("send-soeknad") {
                try {

                    logger.error(call.receiveParameters().toString())

                    val (partisjon, offset) = call.receiveParameters().let {
                        producer.publiser(
                            requireNotNull(UUID.randomUUID().toString()),
                            opprettSoeknadJson(
                                gjenlevendeFnr = it["fnrGjenlevende"]!!,
                                avdoedFnr = it["fnrAvdoed"]!!,
                                barnFnr = it["fnrBarn"]!!
                            ),
                            mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                        )
                    }
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    // call.respondRedirect("/$path/sendt?partisjon=$partisjon&offset=$offset")
                    call.respond("Søknad er sendt")

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
        }
}

private fun opprettSoeknadJson(gjenlevendeFnr: String, avdoedFnr: String, barnFnr: String): String {
    val skjemaInfo = opprettSkjemaInfo(gjenlevendeFnr, barnFnr, avdoedFnr)

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

const val bestilling = """
{
  "antall": 1,
  "beskrivelse": null,
  "pdldata": {
    "opprettNyPerson": {
      "identtype": "FNR",
      "foedtEtter": null,
      "foedtFoer": null,
      "alder": 40,
      "syntetisk": true
    },
    "person": {
      "navn": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "etternavn": null,
          "fornavn": null,
          "mellomnavn": null,
          "hasMellomnavn": false
        }
      ],
      "foedsel": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "foedekommune": null,
          "foedeland": null,
          "foedested": null,
          "foedselsaar": null,
          "foedselsdato": null
        }
      ],
      "forelderBarnRelasjon": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "minRolleForPerson": "FORELDER",
          "relatertPerson": null,
          "relatertPersonsRolle": "BARN",
          "relatertPersonUtenFolkeregisteridentifikator": null,
          "borIkkeSammen": null,
          "nyRelatertPerson": {
            "identtype": "FNR",
            "kjoenn": "KVINNE",
            "foedtEtter": null,
            "foedtFoer": null,
            "alder": 10,
            "syntetisk": false,
            "nyttNavn": {
              "hasMellomnavn": false
            },
            "statsborgerskapLandkode": "NOR",
            "gradering": null,
            "eksisterendeIdent": null
          },
          "partnerErIkkeForelder": false,
          "eksisterendePerson": false,
          "deltBosted": null,
          "typeForelderBarn": "NY"
        }
      ],
      "sivilstand": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "bekreftelsesdato": null,
          "relatertVedSivilstand": null,
          "sivilstandsdato": "2015-08-12T00:00:00",
          "type": "GIFT",
          "borIkkeSammen": false,
          "nyRelatertPerson": {
            "identtype": null,
            "kjoenn": null,
            "foedtEtter": null,
            "foedtFoer": null,
            "alder": null,
            "syntetisk": false,
            "nyttNavn": {
              "hasMellomnavn": false
            },
            "statsborgerskapLandkode": null,
            "gradering": null,
            "eksisterendeIdent": null
          },
          "eksisterendePerson": false
        }
      ],
      "doedsfall": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "PDL",
          "folkeregistermetadata": null,
          "doedsdato": "2022-08-17T09:14:07"
        }
      ],
      "foreldreansvar": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "ansvar": "FELLES",
          "ansvarlig": null,
          "nyAnsvarlig": null,
          "ansvarligUtenIdentifikator": null,
          "gyldigFraOgMed": null,
          "gyldigTilOgMed": null,
          "eksisterendePerson": false
        }
      ]
    }
  },
  "importPersoner": null,
  "antallIdenter": 1,
  "navSyntetiskIdent": false,
  "environments": []
}
"""

private fun opprettSkjemaInfo(
    gjenlevendeFnr: String,
    barnFnr: String,
    avdoedFnr: String
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
      "mottattDato": "2022-02-10T10:51:12.298943803",
      "template": "barnepensjon_v2"
    }
""".trimIndent()
