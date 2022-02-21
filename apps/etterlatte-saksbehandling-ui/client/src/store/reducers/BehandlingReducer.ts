import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: string
  sak: number
  grunnlag: IBehandlingsopplysning[]
  vilkårsprøving: IVilkaarsproving[]
  beregning: any
  fastsatt: boolean
}

export interface IBehandlingsopplysning {
  id: string
  kilde: string
  opplysningsType: string
  meta: any
  opplysning: any
  attestering: any
}

export interface IVilkaarsproving {
  navn: VilkaarsType
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IKriterie
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
}

export enum VilkaarVurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export interface IKriterie {
  navn: Kriterietype
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IBehandlingsopplysning[]
}

export enum Kriterietype {
  AVDOED_ER_FORELDER = 'AVDOED_ER_FORELDER',
  DOEDSFALL_ER_REGISTRERT_I_PDL = 'DOEDSFALL_ER_REGISTRERT_I_PDL',
  SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO = 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO',
}

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  grunnlag: [],
  vilkårsprøving: [],
  beregning: undefined,
  fastsatt: false,
}

export const behandlingReducer = (state = detaljertBehandlingInitialState, action: IAction): any => {
  console.log('state', action.data)
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
