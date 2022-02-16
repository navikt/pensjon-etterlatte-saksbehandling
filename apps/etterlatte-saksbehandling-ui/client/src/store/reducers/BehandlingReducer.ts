import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: number
  sak: number
  grunnlag: IBehandlingsopplysning[]
  vilkaarsproving: IVilkaarsproving[]
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
  navn: string
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IBehandlingsopplysning[]
}

export enum VilkaarVurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: 0,
  sak: 0,
  grunnlag: [],
  vilkaarsproving: [],
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
