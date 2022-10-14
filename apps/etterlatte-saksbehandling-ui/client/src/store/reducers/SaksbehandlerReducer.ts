import { createAction, createReducer } from '@reduxjs/toolkit'

export const initialState: SaksbehandlerReducer = {
  saksbehandler: {
    ident: '',
    navn: '',
    fornavn: '',
    etternavn: '',
    enheter: [],
    rolle: '', //test
  },
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

export const setSaksbehandler = createAction<ISaksbehandler>('saksbehandler/set')
export interface SaksbehandlerReducer {
  saksbehandler: ISaksbehandler
}
export const saksbehandlerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandler, (state, action) => {
    state.saksbehandler = action.payload
  })
})
