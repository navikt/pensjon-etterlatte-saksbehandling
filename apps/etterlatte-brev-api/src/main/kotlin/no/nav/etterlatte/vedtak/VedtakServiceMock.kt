package no.nav.etterlatte.vedtak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Sak
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*
import kotlin.random.Random

class VedtakServiceMock : VedtakService {

    companion object {
        private const val PORSGRUNN = "0805"
    }

    override fun hentVedtak(behandlingId: String) = Vedtak(
        vedtakId = Random.nextLong(),
        virk = Periode(YearMonth.of(2022, 1), null),
        sak = Sak("11057523044", "barnepensjon", 100L),
        behandling = Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, id = UUID.randomUUID()),
        type = VedtakType.INNVILGELSE,
        grunnlag = grunnlag(),
        vilkaarsvurdering = Vilkaarsvurdering(
            UUID.randomUUID(),
            emptyList(),
            Virkningstidspunkt(YearMonth.of(2022, 1), Grunnlagsopplysning.Saksbehandler("Z1000", Instant.now())),
            VilkaarsvurderingResultat(VilkaarsvurderingUtfall.OPPFYLT, null, LocalDateTime.now(), "ABCDEF")
        ),
        beregning = null,
        avkorting = null,
        pensjonTilUtbetaling = listOf(
            Utbetalingsperiode(
                20L,
                Periode(YearMonth.of(2022, 1), null),
                BigDecimal.valueOf(2300.00),
                type = UtbetalingsperiodeType.UTBETALING
            )
        ),
        vedtakFattet = VedtakFattet("z12345", ansvarligEnhet = PORSGRUNN, tidspunkt = ZonedDateTime.now()),
        attestasjon = Attestasjon("z54321", attesterendeEnhet = PORSGRUNN, tidspunkt = ZonedDateTime.now())
    )
}

fun grunnlag(): List<Grunnlagsopplysning<ObjectNode>> = """
    [
        {
          "id": "183d768a-9e98-45e1-b9e9-32fd3c86a4bd",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "AVDOED_SOEKNAD_V1",
          "meta": {},
          "opplysning": {
            "type": "AVDOED",
            "fornavn": "fe",
            "etternavn": "fe",
            "foedselsnummer": "22128202440",
            "doedsdato": "2022-01-01",
            "statsborgerskap": "Norge",
            "utenlandsopphold": {
              "harHattUtenlandsopphold": "NEI",
              "opphold": null
            },
            "doedsaarsakSkyldesYrkesskadeEllerYrkessykdom": "NEI"
          },
          "attestering": null
        },
        {
          "id": "cad5569b-be0d-499f-8f1b-e3a8e4aa484c",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "SOEKER_SOEKNAD_V1",
          "meta": {},
          "opplysning": {
            "type": "BARN",
            "fornavn": "kirsten",
            "etternavn": "jakobsen",
            "foedselsnummer": "12101376212",
            "statsborgerskap": "Norge",
            "utenlandsadresse": {
              "adresseIUtlandet": "NEI",
              "land": null,
              "adresse": null
            },
            "foreldre": [
              {
                "type": "FORELDER",
                "fornavn": "GØYAL",
                "etternavn": "HØYSTAKK",
                "foedselsnummer": "12101376212"
              },
              {
                "type": "FORELDER",
                "fornavn": "fe",
                "etternavn": "fe",
                "foedselsnummer": "22128202440"
              }
            ],
            "verge": {
              "barnHarVerge": "NEI",
              "fornavn": null,
              "etternavn": null,
              "foedselsnummer": null
            },
            "omsorgPerson": null
          },
          "attestering": null
        },
        {
          "id": "c0ff69f8-8a82-4b68-95ef-541a33ee6acf",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "GJENLEVENDE_FORELDER_SOEKNAD_V1",
          "meta": {},
          "opplysning": {
            "type": "GJENLEVENDE_FORELDER",
            "fornavn": "GØYAL",
            "etternavn": "HØYSTAKK",
            "foedselsnummer": "03108718357",
            "adresse": "Sannergata 6C, 0557 Oslo",
            "statsborgerskap": "Norge",
            "telefonnummer": "11111111"
          },
          "attestering": null
        },
        {
          "id": "33e9eb39-efc0-4a0a-b595-84cfcf4da715",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "INNSENDER_SOEKNAD_V1",
          "meta": {},
          "opplysning": {
            "type": "INNSENDER",
            "fornavn": "GØYAL",
            "etternavn": "HØYSTAKK",
            "foedselsnummer": "03108718357"
          },
          "attestering": null
        },
        {
          "id": "4fd9ca42-a2f8-4ccf-8615-ce9db6fa11b2",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "UTBETALINGSINFORMASJON_V1",
          "meta": {},
          "opplysning": {
            "bankkontoType": "NORSK",
            "kontonummer": "6848.64.44444",
            "utenlandskBankNavn": null,
            "utenlandskBankAdresse": null,
            "iban": null,
            "swift": null,
            "oenskerSkattetrekk": null,
            "oensketSkattetrekkProsent": null
          },
          "attestering": null
        },
        {
          "id": "46e43b79-21a2-4eab-8cd2-cc7f20587311",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "SAMTYKKE",
          "meta": {},
          "opplysning": {
            "harSamtykket": true
          },
          "attestering": null
        },
        {
          "id": "98b0b5e3-93b9-46ac-b5f6-250e90e25fe0",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "SOEKNAD_MOTTATT_DATO",
          "meta": {},
          "opplysning": {
            "mottattDato": "2022-02-14T14:37:24.573612786"
          },
          "attestering": null
        },
        {
          "id": "ae835327-1545-45e5-ae43-76557df3efae",
          "kilde": {
            "fnr": "03108718357",
            "mottatDato": "2022-02-14T14:37:24.573612786Z",
            "type": "privatperson"
          },
          "opplysningType": "SOEKNADSTYPE_V1",
          "meta": {},
          "opplysning": {
            "type": "BARNEPENSJON"
          },
          "attestering": null
        },
        {
          "id": "40f465ae-5c66-49b3-bea5-48f93e9c952d",
          "kilde": {
            "navn": "pdl",
            "tidspunktForInnhenting": "2022-03-22T11:34:32.449688260Z",
            "registersReferanse": null,
            "type": "pdl"
          },
          "opplysningType": "AVDOED_PDL_V1",
          "meta": {},
          "opplysning": {
            "fornavn": "VAKKER",
            "etternavn": "LAPP",
            "foedselsnummer": "22128202440",
            "foedselsdato": "1982-12-22",
            "foedselsaar": 1982,
            "foedeland": "NOR",
            "doedsdato": "2022-02-10",
            "adressebeskyttelse": "UGRADERT",
            "bostedsadresse": [
              {
                "type": "VEGADRESSE",
                "aktiv": true,
                "coAdresseNavn": null,
                "adresseLinje1": "Bøveien 937",
                "adresseLinje2": null,
                "adresseLinje3": null,
                "postnr": "8475",
                "poststed": null,
                "land": null,
                "kilde": "FREG",
                "gyldigFraOgMed": "1999-01-01T00:00:00",
                "gyldigTilOgMed": null
              }
            ],
            "deltBostedsadresse": null,
            "kontaktadresse": [],
            "oppholdsadresse": [],
            "sivilstatus": "UGIFT",
            "statsborgerskap": "NOR",
            "utland": {
              "innflyttingTilNorge": [],
              "utflyttingFraNorge": []
            },
            "familieRelasjon": {
              "ansvarligeForeldre": null,
              "foreldre": null,
              "barn": [
                "12101376212"
              ]
            }
          },
          "attestering": null
        },
        {
          "id": "b3a5c4cd-02cb-4a34-a5d2-8390d74b0299",
          "kilde": {
            "navn": "pdl",
            "tidspunktForInnhenting": "2022-03-22T11:34:32.449699568Z",
            "registersReferanse": null,
            "type": "pdl"
          },
          "opplysningType": "GJENLEVENDE_FORELDER_PDL_V1",
          "meta": {},
          "opplysning": {
            "fornavn": "BRÅKETE",
            "etternavn": "POTET",
            "foedselsnummer": "03108718357",
            "foedselsdato": "1987-10-03",
            "foedselsaar": 1987,
            "foedeland": "NOR",
            "doedsdato": null,
            "adressebeskyttelse": "UGRADERT",
            "bostedsadresse": [
              {
                "type": "VEGADRESSE",
                "aktiv": true,
                "coAdresseNavn": null,
                "adresseLinje1": "Bøveien 937",
                "adresseLinje2": null,
                "adresseLinje3": null,
                "postnr": "8475",
                "poststed": null,
                "land": null,
                "kilde": "FREG",
                "gyldigFraOgMed": "1999-01-01T00:00:00",
                "gyldigTilOgMed": null
              }
            ],
            "deltBostedsadresse": null,
            "kontaktadresse": null,
            "oppholdsadresse": [],
            "sivilstatus": "UGIFT",
            "statsborgerskap": "NOR",
            "utland": {
              "innflyttingTilNorge": [],
              "utflyttingFraNorge": []
            },
            "familieRelasjon": {
              "ansvarligeForeldre": null,
              "foreldre": null,
              "barn": [
                "12101376212"
              ]
            }
          },
          "attestering": null
        },
        {
          "id": "161b1590-eb02-4e62-9e52-35402a82ffa4",
          "kilde": {
            "navn": "pdl",
            "tidspunktForInnhenting": "2022-03-22T11:34:32.449703380Z",
            "registersReferanse": null,
            "type": "pdl"
          },
          "opplysningType": "SOEKER_PDL_V1",
          "meta": {},
          "opplysning": {
            "fornavn": "TALENTFULL",
            "etternavn": "BLYANT",
            "foedselsnummer": "12101376212",
            "foedselsdato": "2013-10-12",
            "foedselsaar": 2013,
            "foedeland": "NOR",
            "doedsdato": null,
            "adressebeskyttelse": "UGRADERT",
            "bostedsadresse": [
              {
                "type": "VEGADRESSE",
                "aktiv": true,
                "coAdresseNavn": null,
                "adresseLinje1": "Bøveien 937",
                "adresseLinje2": null,
                "adresseLinje3": null,
                "postnr": "8475",
                "poststed": "Oslo",
                "land": "Norge",
                "kilde": "FREG",
                "gyldigFraOgMed": "1999-01-01T00:00:00",
                "gyldigTilOgMed": null
              }
            ],
            "deltBostedsadresse": null,
            "kontaktadresse": [],
            "oppholdsadresse": [],
            "sivilstatus": "UOPPGITT",
            "statsborgerskap": "NOR",
            "utland": {
              "innflyttingTilNorge": [],
              "utflyttingFraNorge": []
            },
            "familieRelasjon": {
              "ansvarligeForeldre": [
                "22128202440",
                "03108718357"
              ],
              "foreldre": [
                "22128202440",
                "03108718357"
              ],
              "barn": null
            }
          },
          "attestering": null
        }
      ]
""".trimIndent().let { objectMapper.readValue(it) }