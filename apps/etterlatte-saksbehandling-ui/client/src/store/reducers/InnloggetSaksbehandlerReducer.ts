import { createAction, createReducer } from '@reduxjs/toolkit'
import { ISaksbehandler } from '~shared/types/saksbehandler'

export const initialState: InnloggetSaksbehandlerReducer = {
  innloggetSaksbehandler: {
    ident: '',
    navn: '',
    fornavn: '',
    etternavn: '',
    enheter: [],
    kanAttestere: false,
  },
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
