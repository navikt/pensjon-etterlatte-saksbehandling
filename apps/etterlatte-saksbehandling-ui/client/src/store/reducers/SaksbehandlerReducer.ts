import { IAction } from '../AppContext'

export const saksbehandlerReducerInitialState: ISaksbehandler = {
  ident: '',
  navn: '',
  fornavn: '',
  etternavn: '',
  enheter: [],
}

interface IEnhet {
  enhetId: string
  navn: string
}

export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
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
      }
    default:
      return state
  }
}
