import { createAction, createReducer } from '@reduxjs/toolkit'
import { InnloggetSaksbehandler } from '~shared/types/saksbehandler'

export const initialState: InnloggetSaksbehandlerReducer = {
  innloggetSaksbehandler: {
    ident: '',
    navn: '',
    enheter: [],
    kanAttestere: false,
    skriveEnheter: [],
    kanSeOppgaveliste: false,
  },
}

export const setSaksbehandler = createAction<InnloggetSaksbehandler>('saksbehandler/set')

export interface InnloggetSaksbehandlerReducer {
  innloggetSaksbehandler: InnloggetSaksbehandler
}
export const saksbehandlerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandler, (state, action) => {
    state.innloggetSaksbehandler = action.payload
  })
})
