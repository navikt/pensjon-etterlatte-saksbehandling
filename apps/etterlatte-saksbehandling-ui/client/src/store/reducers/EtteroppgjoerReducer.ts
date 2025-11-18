import { createAction, createReducer } from '@reduxjs/toolkit'
import {
  BeregnetEtteroppgjoerResultatDto,
  EtteroppgjoerForbehandling,
  DetaljertEtteroppgjoerForbehandling,
} from '~shared/types/EtteroppgjoerForbehandling'
import { OppgaveDTO } from '~shared/types/oppgave'
import { useAppSelector } from '~store/Store'
import { IBrev } from '~shared/types/Brev'

export const addEtteroppgjoer = createAction<DetaljertEtteroppgjoerForbehandling>('etteroppgjoer/add')
export const addEtteroppgjoerOppgave = createAction<OppgaveDTO>('etteroppgjoer/oppgave/add')
export const addEtteroppgjoerBrev = createAction<IBrev>('etteroppgjoer/brev/add')
export const resetEtteroppgjoer = createAction('etteroppgjoer/reset')
export const addResultatEtteroppgjoer = createAction<BeregnetEtteroppgjoerResultatDto>('etteroppgjoer/resultat/add')
export const updateEtteroppgjoerBehandling = createAction<EtteroppgjoerForbehandling>('etteroppgjoer/behandling/update')
const initialState: {
  etteroppgjoer: DetaljertEtteroppgjoerForbehandling | null
  oppgave: OppgaveDTO | null
} = {
  etteroppgjoer: null,
  oppgave: null,
}

export const etteroppgjoerReducer = createReducer(initialState, (builder) => {
  builder.addCase(addEtteroppgjoer, (state, action) => {
    state.etteroppgjoer = action.payload
  })
  builder.addCase(addEtteroppgjoerBrev, (state, action) => {
    if (state.etteroppgjoer?.forbehandling) {
      state.etteroppgjoer.forbehandling.brevId = action.payload.id
    }
  })
  builder.addCase(updateEtteroppgjoerBehandling, (state, action) => {
    if (state.etteroppgjoer?.forbehandling) {
      state.etteroppgjoer.forbehandling = action.payload
    }
  })
  builder.addCase(addEtteroppgjoerOppgave, (state, action) => {
    state.oppgave = action.payload
  })
  builder.addCase(resetEtteroppgjoer, (state) => {
    state.etteroppgjoer = null
    state.oppgave = null
  })
  builder.addCase(addResultatEtteroppgjoer, (state, action) => {
    state.etteroppgjoer!!.beregnetEtteroppgjoerResultat = action.payload
  })
})

export function useEtteroppgjoer() {
  return useAppSelector((state) => state.etteroppgjoerReducer.etteroppgjoer!)
}
