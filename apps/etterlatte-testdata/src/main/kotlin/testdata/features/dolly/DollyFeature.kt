package testdata.features.dolly


import dolly.DollyService
import io.ktor.server.application.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.getClientAccessToken
import no.nav.etterlatte.logger
import no.nav.etterlatte.usernameFraToken

class DollyFeature(private val dollyService: DollyService) : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad"
    override val path: String
        get() = "dolly"

    override val routes: Route.() -> Unit
        get() = {
            get {
                val gruppeId = dollyService.hentTestGruppe(usernameFraToken()!!, getClientAccessToken())
                call.respond(
                    MustacheContent(
                        "soeknad/dolly.hbs", mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                            "gruppeId" to gruppeId
                        )
                    )
                )
            }

            get("opprett-familie") {
                try {
                    val accessToken = getClientAccessToken()
                    dollyService.hentTestGruppe(usernameFraToken()!!, accessToken)?.let { id ->
                        dollyService.opprettBestilling(bestilling, id, accessToken).also { bestilling ->
                            logger.info("Bestilling med id ${bestilling.id} har status ${bestilling.ferdig}")
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
        }
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