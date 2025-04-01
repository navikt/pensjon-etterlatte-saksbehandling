import { createAction, createReducer } from '@reduxjs/toolkit'
import { BeregnetEtteroppgjoerResultatDto, Etteroppgjoer } from '~shared/types/Etteroppgjoer'
import { OppgaveDTO } from '~shared/types/oppgave'
import { useAppSelector } from '~store/Store'

export const addEtteroppgjoer = createAction<Etteroppgjoer>('etteroppgjoer/add')
export const addEtteroppgjoerOppgave = createAction<OppgaveDTO>('etteroppjoer/oppgave/add')
export const resetEtteroppgjoer = createAction('etteroppgjoer/reset')
export const addResultatEtteroppgjoer = createAction<BeregnetEtteroppgjoerResultatDto>('etteroppgjoer/resultat/add')

const initialState: {
  etteroppgjoer: Etteroppgjoer | null
  oppgave: OppgaveDTO | null
  resultat: BeregnetEtteroppgjoerResultatDto | null
} = {
  etteroppgjoer: null,
  oppgave: null,
  resultat: null,
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
  builder.addCase(addResultatEtteroppgjoer, (state, action) => {
    state.resultat = action.payload
  })
})

export function useEtteroppgjoer() {
  return useAppSelector((state) => state.etteroppgjoerReducer.etteroppgjoer!)
}
