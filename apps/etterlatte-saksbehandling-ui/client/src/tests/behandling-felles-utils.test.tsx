import React from 'react'
import { hentAdresserEtterDoedsdato, hentKriterier } from '../components/behandling/felles/utils'
import { IAdresse } from '../components/behandling/soeknadsoversikt/types'
import { Kriterietype, OpplysningsType } from '../store/reducers/BehandlingReducer';

const adresserMock: IAdresse[] = [
  {
    type: 'VEGADRESSE',
    aktiv: false,
    coAdresseNavn: null,
    adresseLinje1: 'Bøveien 937',
    adresseLinje2: null,
    adresseLinje3: null,
    postnr: '8475',
    poststed: null,
    land: null,
    kilde: 'FREG',
    gyldigFraOgMed: '1999-01-01',
    gyldigTilOgMed: '2003-01-03',
  },
  {
    type: 'VEGADRESSE',
    aktiv: true,
    coAdresseNavn: null,
    adresseLinje1: 'Knallveien 7',
    adresseLinje2: null,
    adresseLinje3: null,
    postnr: '8475',
    poststed: null,
    land: null,
    kilde: 'FREG',
    gyldigFraOgMed: '1999-01-01',
    gyldigTilOgMed: '2003-01-03',
  },
];

const vilkaarMock = {
  "navn": "DOEDSFALL_ER_REGISTRERT",
  "resultat": "OPPFYLT",
  "kriterier": [
      {
          "navn": "DOEDSFALL_ER_REGISTRERT_I_PDL",
          "resultat": "OPPFYLT",
          "basertPaaOpplysninger": [
              {
                  "opplysningsType": "AVDOED_DOEDSFALL_V1",
                  "kilde": {
                      "navn": "pdl",
                      "tidspunktForInnhenting": "2022-03-07T14:09:33.789469374Z",
                      "registersReferanse": null,
                      "type": "pdl"
                  },
                  "opplysning": {
                      "doedsdato": "2022-02-10",
                      "foedselsnummer": "22128202440"
                  }
              }
          ]
      },
      {
          "navn": "AVDOED_ER_FORELDER",
          "resultat": "OPPFYLT",
          "basertPaaOpplysninger": [
              {
                  "opplysningsType": "SOEKER_RELASJON_FORELDRE_V1",
                  "kilde": {
                      "navn": "pdl",
                      "tidspunktForInnhenting": "2022-03-07T14:09:34.766506822Z",
                      "registersReferanse": null,
                      "type": "pdl"
                  },
                  "opplysning": {
                      "foreldre": [
                          {
                              "fornavn": "VAKKER",
                              "etternavn": "LAPP",
                              "foedselsnummer": "22128202440",
                              "adresse": "Adresse",
                              "type": "FORELDER"
                          },
                          {
                              "fornavn": "BRÅKETE",
                              "etternavn": "POTET",
                              "foedselsnummer": "03108718357",
                              "adresse": "Adresse",
                              "type": "FORELDER"
                          }
                      ]
                  }
              },
              {
                  "opplysningsType": "AVDOED_DOEDSFALL_V1",
                  "kilde": {
                      "navn": "pdl",
                      "tidspunktForInnhenting": "2022-03-07T14:09:33.789469374Z",
                      "registersReferanse": null,
                      "type": "pdl"
                  },
                  "opplysning": {
                      "doedsdato": "2022-02-10",
                      "foedselsnummer": "22128202440"
                  }
              }
          ]
      }
  ]
}


describe('Behandling-felles-utils', () => {
  it('Test at adresse ikke henter ut inaktiv adresse', () => {
    const adresser = hentAdresserEtterDoedsdato(adresserMock, new Date('2015-01-01')) //
    expect(adresser.length).toBe(1)
  })

  it('Test at adresse er utgått men aktiv', () => {
    const adresser = hentAdresserEtterDoedsdato(adresserMock, new Date('2015-01-01')) //
    expect(adresser.length).toBe(1)
    expect(adresser[0].adresseLinje1).toBe('Knallveien 7')
  })

  it('Test at hent kriterier returnerer riktig', () => {
    const vilkaarResult = hentKriterier(vilkaarMock, Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL, OpplysningsType.avdoed_doedsfall)
    expect(vilkaarResult.opplysningsType).toBe(OpplysningsType.avdoed_doedsfall)
  })
})
