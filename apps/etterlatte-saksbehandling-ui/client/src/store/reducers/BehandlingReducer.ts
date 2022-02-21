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
  id: "",
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
