import { createAction, createReducer } from '@reduxjs/toolkit'
import { SaksbehandlerMedInformasjon } from '~shared/types/saksbehandler'

export const initialState: InnloggetSaksbehandlerReducer = {
  innloggetSaksbehandler: {
    ident: '',
    navn: '',
    enheter: [],
    kanAttestere: false,
    skriveTilgang: false,
    leseTilgang: false,
  },
}

export const setSaksbehandler = createAction<SaksbehandlerMedInformasjon>('saksbehandler/set')

export interface InnloggetSaksbehandlerReducer {
  innloggetSaksbehandler: SaksbehandlerMedInformasjon
}
export const saksbehandlerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandler, (state, action) => {
    state.innloggetSaksbehandler = action.payload
  })
})
