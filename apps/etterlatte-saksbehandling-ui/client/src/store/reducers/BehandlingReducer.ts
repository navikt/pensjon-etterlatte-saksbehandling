import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: string
  sak: number
  grunnlag: IBehandlingsopplysning[]
  vilkårsprøving: IVilkaarResultat
  beregning: any
  fastsatt: boolean
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

export interface IVilkaarResultat {
  resultat: VilkaarVurderingsResultat | undefined
  vilkaar: IVilkaarsproving[]
}
export interface IVilkaarsproving {
  navn: VilkaarsType
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IKriterie[]
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
  AVDOEDES_FORUTGAAENDE_MELDLEMSKAP = 'AVDOEDES_FORUTGAAENDE_MELDLEMSKAP',
  BARNETS_MEDLEMSKAP = 'BARNETS_MEDLEMSKAP',
}

export enum VilkaarVurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export interface IKriterie {
  navn: Kriterietype
  resultat: VilkaarVurderingsResultat
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

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  grunnlag: [],
  vilkårsprøving: { resultat: undefined, vilkaar: [] },
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
