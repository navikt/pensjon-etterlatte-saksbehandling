import { createAction, createReducer } from '@reduxjs/toolkit'

export const setSaksbehandlerGjeldendeOppgave = createAction<string | null>('saksbehandlerGjeldendeOppgave/set')
export const resetSaksbehandlerGjeldendeOppgave = createAction('saksbehandlerGjeldendeOppgave/reset')

const initialState: { saksbehandler: string | null } = {
  saksbehandler: null,
}

export const saksbehandlerGjeldendeOppgaveForBehandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(setSaksbehandlerGjeldendeOppgave, (state, action) => {
    state.saksbehandler = action.payload
  })
  builder.addCase(resetSaksbehandlerGjeldendeOppgave, (state) => {
    state.saksbehandler = null
  })
})
