import { createAction, createReducer } from '@reduxjs/toolkit'
import { OppgaveDTO } from '~shared/types/oppgave'

export const settOppgave = createAction<OppgaveDTO | null>('oppgave/set')
export const resetOppgave = createAction('oppgave/reset')

const initialState: { oppgave: OppgaveDTO | null } = {
  oppgave: null,
}

export const oppgaveReducer = createReducer(initialState, (builder) => {
  builder.addCase(settOppgave, (state, action) => {
    state.oppgave = action.payload
  })
  builder.addCase(resetOppgave, (state) => {
    state.oppgave = null
  })
})
