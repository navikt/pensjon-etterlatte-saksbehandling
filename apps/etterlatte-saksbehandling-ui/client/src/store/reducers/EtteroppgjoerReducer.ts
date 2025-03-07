import { createAction, createReducer } from '@reduxjs/toolkit'
import { Etteroppgjoer } from '~shared/types/Etteroppgjoer'
import { OppgaveDTO } from '~shared/types/oppgave'
import { useAppSelector } from '~store/Store'

export const addEtteroppgjoer = createAction<Etteroppgjoer>('etteroppgjoer/add')
export const addEtteroppgjoerOppgave = createAction<OppgaveDTO>('etteroppjoer/oppgave/add')
export const resetEtteroppgjoer = createAction('etteroppgjoer/reset')

const initialState: { etteroppgjoer: Etteroppgjoer | null; oppgave: OppgaveDTO | null } = {
  etteroppgjoer: null,
  oppgave: null,
}

export const etteroppgjoerReducer = createReducer(initialState, (builder) => {
  builder.addCase(addEtteroppgjoer, (state, action) => {
    state.etteroppgjoer = action.payload
  })
  builder.addCase(addEtteroppgjoerOppgave, (state, action) => {
    state.oppgave = action.payload
  })
  builder.addCase(resetEtteroppgjoer, (state) => {
    state.etteroppgjoer = null
    state.oppgave = null
  })
})

export function useEtteroppgjoer() {
  return useAppSelector((state) => state.etteroppgjoerReducer.etteroppgjoer!)
}
