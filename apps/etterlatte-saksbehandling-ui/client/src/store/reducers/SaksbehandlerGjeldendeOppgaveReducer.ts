import { createAction, createReducer } from '@reduxjs/toolkit'

export const setSaksbehandlerGjeldendeOppgave = createAction<string | null>('saksbehandlerGjeldendeOppgave/set')

const initialState: { saksbehandler: string | null } = {
  saksbehandler: null,
}

export const saksbehandlerGjeldendeOppgaveReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandlerGjeldendeOppgave, (state, action) => {
    state.saksbehandler = action.payload
  })
})
