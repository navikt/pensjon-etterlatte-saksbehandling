import { IAction } from '../AppContext'

export const saksbehandlerReducerInitialState: ISaksbehandler = {
  ident: '',
  navn: '',
  fornavn: '',
  etternavn: '',
  enheter: [],
  rolle: '', //test
}

interface IEnhet {
  enhetId: string
  navn: string
}

export enum IRolle {
  saksbehandler = 'saksbehandler',
  attestant = 'attestant',
}

export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
  rolle: string //test
}

export const saksbehandlerReducer = (state: ISaksbehandler, action: IAction): ISaksbehandler => {
  switch (action.type) {
    case 'setSaksbehandler':
      return {
        ...state,
        ident: action.data.ident,
        navn: action.data.navn,
        fornavn: action.data.fornavn,
        etternavn: action.data.etternavn,
        enheter: action.data.enheter,
        rolle: action.data.rolle, //test
      }
    default:
      return state
  }
}
