package no.nav.etterlatte.testdata.features.dolly

import no.nav.etterlatte.testdata.dolly.BestillingRequest
import java.time.LocalDateTime
import kotlin.random.Random

fun generererBestilling(bestilling: BestillingRequest): String {
    val soeker =
        soeskenTemplate(
            helsoesken = true,
            erOver18 = bestilling.erOver18,
            kjoenn = vilkaarligKjoenn(),
        )
    val helsoesken =
        List(bestilling.helsoesken) {
            soeskenTemplate(
                true,
                kjoenn = vilkaarligKjoenn(),
            )
        }
    val halvsoeskenAvdoed =
        List(bestilling.halvsoeskenAvdoed) {
            soeskenTemplate(
                false,
                kjoenn = vilkaarligKjoenn(),
            )
        }

    val barnListe = listOf(listOf(soeker), helsoesken, halvsoeskenAvdoed).flatten()

    return bestillingTemplateStart(
        bestilling.avdoedAlder,
        bestilling.antall,
    ) + barnListe +
        bestillingTemplateEnd(
            LocalDateTime.now().minusDays(bestilling.antallDagerSidenDoedsfall.toLong()),
            bestilling.gjenlevendeAlder,
        )
}

fun alderVoksenRandom(): Int = Random.nextInt(20, 60)

private fun alder18Til20() = Random.nextInt(18, 20)

private fun alderUnder18() = Random.nextInt(1, 18)

private fun vilkaarligKjoenn() = if (Random.nextBoolean()) "KVINNE" else "MANN"

fun bestillingTemplateStart(
    alder: Int,
    antall: Int,
) = """
{
  "antall": $antall,
  "beskrivelse": null,
  "pdldata": {
    "opprettNyPerson": {
      "identtype": "FNR",
      "foedtEtter": null,
      "foedtFoer": null,
      "alder": $alder,
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
      "foedested": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "foedekommune": null,
          "foedeland": null,
          "foedested": null
        }
      ],
      "foedselsdato": [
        {
          "id": null,
          "kilde": "Dolly",
          "master": "FREG",
          "folkeregistermetadata": null,
          "foedselsaar": null,
          "foedselsdato": null
        }
      ],
      "forelderBarnRelasjon": """

fun bestillingTemplateEnd(
    doedsdato: LocalDateTime,
    gjenlevendeAlder: Int? = null,
) = """,
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
            "alder": $gjenlevendeAlder,
            "syntetisk": true,
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
          "doedsdato": "$doedsdato"
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

private fun soeskenTemplate(
    helsoesken: Boolean,
    erOver18: Boolean = false,
    kjoenn: String,
) = """
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
    "kjoenn": "$kjoenn",
    "foedtEtter": null,
    "foedtFoer": null,
    "alder": ${
    when (erOver18) {
        true -> "${alder18Til20()}"
        false -> "${alderUnder18()}"
    }
},
    "syntetisk": true,
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
