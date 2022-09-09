import { hentAdresserEtterDoedsdato, hentKriterierMedOpplysning } from '../components/behandling/felles/utils'
import {
  IAdresse,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../store/reducers/BehandlingReducer'

const adresserMock: IAdresse[] = [
  {
    type: 'VEGADRESSE',
    aktiv: false,
    coAdresseNavn: undefined,
    adresseLinje1: 'Bøveien 937',
    adresseLinje2: undefined,
    adresseLinje3: undefined,
    postnr: '8475',
    poststed: undefined,
    land: undefined,
    kilde: 'FREG',
    gyldigFraOgMed: '1999-01-01',
    gyldigTilOgMed: '2003-01-03',
  },
  {
    type: 'VEGADRESSE',
    aktiv: true,
    coAdresseNavn: undefined,
    adresseLinje1: 'Knallveien 7',
    adresseLinje2: undefined,
    adresseLinje3: undefined,
    postnr: '8475',
    poststed: undefined,
    land: undefined,
    kilde: 'FREG',
    gyldigFraOgMed: '1999-01-01',
    gyldigTilOgMed: undefined,
  },
]

const vilkaarMock: IVilkaarsproving = {
  navn: VilkaarsType.DOEDSFALL_ER_REGISTRERT,
  resultat: VurderingsResultat.OPPFYLT,
  utfall: undefined,
  kriterier: [
    {
      navn: Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL,
      resultat: VurderingsResultat.OPPFYLT,
      basertPaaOpplysninger: [
        {
          kriterieOpplysningsType: KriterieOpplysningsType.DOEDSDATO,
          kilde: {
            navn: 'pdl',
            tidspunktForInnhenting: '2022-03-07T14:09:33.789469374Z',
            registersReferanse: null,
            type: 'pdl',
          },
          opplysning: {
            doedsdato: '2022-02-10',
            foedselsnummer: '22128202440',
          },
        },
      ],
    },
    {
      navn: Kriterietype.AVDOED_ER_FORELDER,
      resultat: VurderingsResultat.OPPFYLT,
      basertPaaOpplysninger: [
        {
          kriterieOpplysningsType: KriterieOpplysningsType.FORELDRE,
          kilde: {
            navn: 'pdl',
            tidspunktForInnhenting: '2022-03-07T14:09:34.766506822Z',
            registersReferanse: null,
            type: 'pdl',
          },
          opplysning: {
            foreldre: [
              {
                fornavn: 'VAKKER',
                etternavn: 'LAPP',
                foedselsnummer: '22128202440',
                adresse: 'Adresse',
                type: 'FORELDER',
              },
              {
                fornavn: 'BRÅKETE',
                etternavn: 'POTET',
                foedselsnummer: '03108718357',
                adresse: 'Adresse',
                type: 'FORELDER',
              },
            ],
          },
        },
        {
          kriterieOpplysningsType: KriterieOpplysningsType.DOEDSDATO,
          kilde: {
            navn: 'pdl',
            tidspunktForInnhenting: '2022-03-07T14:09:33.789469374Z',
            registersReferanse: null,
            type: 'pdl',
          },
          opplysning: {
            doedsdato: '2022-02-10',
            foedselsnummer: '22128202440',
          },
        },
      ],
    },
  ],
  vurdertDato: '2022-03-30T14:36:17.984583465Z',
}

describe('Behandling-felles-utils', () => {
  it('Test at adresse ikke henter ut inaktiv adresse', () => {
    const adresser = hentAdresserEtterDoedsdato(adresserMock, '2015-01-01')
    expect(adresser.length).toBe(1)
  })

  it('Test at adresse er utgått men aktiv', () => {
    const adresser = hentAdresserEtterDoedsdato(adresserMock, '2015-01-01')
    expect(adresser.length).toBe(1)
    expect(adresser[0].adresseLinje1).toBe('Knallveien 7')
  })

  it('Test at hent kriterier returnerer riktig', () => {
    const vilkaarResult = hentKriterierMedOpplysning(
      vilkaarMock,
      Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL,
      KriterieOpplysningsType.DOEDSDATO
    )
    expect(vilkaarResult?.kriterieOpplysningsType).toBe(KriterieOpplysningsType.DOEDSDATO)
  })
})
