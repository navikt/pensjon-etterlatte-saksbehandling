package no.nav.etterlatte.testdata.features.dolly

import no.nav.etterlatte.testdata.dolly.BestillingRequest

fun generererBestilling(bestilling: BestillingRequest): String {
    val soeker = soeskenTemplate(true)
    val helsoesken = List(bestilling.helsoesken) { soeskenTemplate(true) }
    val halvsoeskenAvdoed = List(bestilling.halvsoeskenAvdoed) { soeskenTemplate(false) }

    val barnListe = listOf(listOf(soeker), helsoesken, halvsoeskenAvdoed).flatten()

    return BESTLLING_TEMPLATE_START + barnListe + BESTLLING_TEMPLATE_END
}

const val BESTLLING_TEMPLATE_START = """
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
      "forelderBarnRelasjon": """

const val BESTLLING_TEMPLATE_END = """,
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

private fun soeskenTemplate(helsoesken: Boolean) =
    """
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
    "kjoenn": "MANN",
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
  "partnerErIkkeForelder": ${!helsoesken},
  "eksisterendePerson": false,
  "deltBosted": null,
  "typeForelderBarn": "NY"
}
"""
