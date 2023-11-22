import { createAction, createReducer } from '@reduxjs/toolkit'

export const initialState: InnloggetSaksbehandlerReducer = {
  innloggetSaksbehandler: {
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

//TODO: flytte til user.ts?
export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
  rolle: string //test
}

export const setSaksbehandler = createAction<ISaksbehandler>('saksbehandler/set')
export interface InnloggetSaksbehandlerReducer {
  innloggetSaksbehandler: ISaksbehandler
}
export const saksbehandlerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandler, (state, action) => {
    state.innloggetSaksbehandler = action.payload
  })
})
