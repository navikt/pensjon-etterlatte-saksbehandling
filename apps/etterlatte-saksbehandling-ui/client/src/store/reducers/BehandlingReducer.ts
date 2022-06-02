import { IAction } from '../AppContext'
import { IAdresse } from '../../components/behandling/types'

export interface IDetaljertBehandling {
  id: string
  sak: number
  grunnlag: IBehandlingsopplysning[]
  vilkårsprøving: IVilkaarResultat
  gyldighetsprøving: IGyldighetResultat
  kommerSoekerTilgode: IKommerSoekerTilgode
  beregning: any
  fastsatt: boolean
}

export interface IKommerSoekerTilgode {
  kommerSoekerTilgodeVurdering: IVilkaarResultat
  familieforhold: IFamiliemedlemmer
}

export interface IFamiliemedlemmer {
  avdoed: IPersoninfoAvdoed
  soeker: IPersoninfoSoeker
  gjenlevendeForelder: IPersoninfoGjenlevendeForelder
}

export interface IPersoninfoAvdoed {
  navn: string
  fnr: string
  rolle: PersonRolle
  adresser: IAdresser
  doedsdato: string
}

export interface IPersoninfoSoeker {
  navn: string
  fnr: string
  rolle: PersonRolle
  adresser: IAdresser
  foedselsdato: string
}

export interface IPersoninfoGjenlevendeForelder {
  navn: string
  fnr: string
  rolle: PersonRolle
  adresser: IAdresser
  adresseSoeknad: string
}

export interface IAdresser {
  bostedadresse: IAdresse[]
  oppholdadresse: IAdresse[]
  kontaktadresse: IAdresse[]
}

export interface IBehandlingsopplysning {
  id: string
  kilde: {
    type: KildeType
  }
  opplysningsType: OpplysningsType
  opplysningType?: any
  meta: any
  opplysning: any
  attestering: any
}

export enum OpplysningsType {
  soeker_pdl = 'SOEKER_PDL_V1',
  soeker_soeknad = 'SOEKER_SOEKNAD_V1',
  gjenlevende_forelder_pdl = 'GJENLEVENDE_FORELDER_PDL_V1',
  gjenlevende_forelder_soeknad = 'GJENLEVENDE_FORELDER_SOEKNAD_V1',
  avdoed_forelder_pdl = 'AVDOED_PDL_V1',
  avdoed_forelder_soeknad = 'AVDOED_SOEKNAD_V1',

  soeknad_mottatt = 'SOEKNAD_MOTTATT_DATO',
  innsender = 'INNSENDER_SOEKNAD_V1',
}

export enum KildeType {
  pdl = 'pdl',
  privatperson = 'privatperson',
}

export enum VurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export interface IGyldighetResultat {
  resultat: VurderingsResultat | undefined
  vurderinger: IGyldighetproving[]
  vurdertDato: string
}

export interface IGyldighetproving {
  navn: GyldigFramsattType
  resultat: VurderingsResultat
  basertPaaOpplysninger: any
}

export enum GyldigFramsattType {
  INNSENDER_ER_FORELDER = 'INNSENDER_ER_FORELDER',
  HAR_FORELDREANSVAR_FOR_BARNET = 'HAR_FORELDREANSVAR_FOR_BARNET',
}

export interface IVilkaarResultat {
  resultat: VurderingsResultat | undefined
  vilkaar: IVilkaarsproving[]
  vurdertDato: string
}

export interface IVilkaarsproving {
  navn: VilkaarsType
  resultat: VurderingsResultat
  basertPaaOpplysninger: IKriterie[]
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
  AVDOEDES_FORUTGAAENDE_MEDLEMSKAP = 'AVDOEDES_FORUTGAAENDE_MEDLEMSKAP',
  BARNETS_MEDLEMSKAP = 'BARNETS_MEDLEMSKAP',
  SAMME_ADRESSE = 'SAMME_ADRESSE',
}

export interface IKriterie {
  navn: Kriterietype
  resultat: VurderingsResultat
  basertPaaOpplysninger: IKriterieOpplysning[]
}

export enum Kriterietype {
  AVDOED_ER_FORELDER = 'AVDOED_ER_FORELDER',
  DOEDSFALL_ER_REGISTRERT_I_PDL = 'DOEDSFALL_ER_REGISTRERT_I_PDL',
  SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO = 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO',
  SOEKER_IKKE_ADRESSE_I_UTLANDET = 'SOEKER_IKKE_ADRESSE_I_UTLANDET',
  GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET = 'GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET',
}

export interface IKriterieOpplysning {
  kriterieOpplysningsType: KriterieOpplysningsType
  kilde: any
  opplysning: any
}

export enum KriterieOpplysningsType {
  ADRESSER = 'ADRESSER',
  DOEDSDATO = 'DOEDSDATO',
  FOEDSELSDATO = 'FOEDSELSDATO',
  FORELDRE = 'FORELDRE',
  AVDOED_UTENLANDSOPPHOLD = 'AVDOED_UTENLANDSOPPHOLD',
  SOEKER_UTENLANDSOPPHOLD = 'SOEKER_UTENLANDSOPPHOLD',
}

export interface IPerson {
  type: PersonType
  fornavn: string
  etternavn: string
  foedselsnummer: string
}

export enum PersonType {
  INNSENDER = 'INNSENDER',
  GJENLEVENDE = ' GJENLEVENDE',
  GJENLEVENDE_FORELDER = 'GJENLEVENDE_FORELDER',
  AVDOED = 'AVDOED',
  SAMBOER = 'SAMBOER',
  VERGE = 'VERGE',
  BARN = 'BARN',
  FORELDER = 'FORELDER',
}

export enum PersonRolle {
  BARN = 'BARN',
  AVDOED = 'AVDOED',
  GJENLEVENDE = 'GJENLEVENDE',
}

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  grunnlag: [],
  vilkårsprøving: { resultat: undefined, vilkaar: [], vurdertDato: '' },
  gyldighetsprøving: { resultat: undefined, vurderinger: [], vurdertDato: '' },
  kommerSoekerTilgode: {
    kommerSoekerTilgodeVurdering: { resultat: undefined, vilkaar: [], vurdertDato: '' },
    familieforhold: {
      avdoed: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        adresser: { bostedadresse: [], oppholdadresse: [], kontaktadresse: [] },
        doedsdato: '',
      },
      soeker: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        adresser: { bostedadresse: [], oppholdadresse: [], kontaktadresse: [] },
        foedselsdato: '',
      },
      gjenlevendeForelder: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        adresser: { bostedadresse: [], oppholdadresse: [], kontaktadresse: [] },
        adresseSoeknad: '',
      },
    },
  },
  beregning: undefined,
  fastsatt: false,
}

export const behandlingReducer = (state = detaljertBehandlingInitialState, action: IAction): any => {
  switch (action.type) {
    case 'add_behandling':
      return {
        ...state,
        ...action.data,
      }
    default:
      state
  }
}
