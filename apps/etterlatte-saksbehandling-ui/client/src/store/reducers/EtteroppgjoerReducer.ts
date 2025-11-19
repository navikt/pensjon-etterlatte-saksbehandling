import { createAction, createReducer } from '@reduxjs/toolkit'
import {
  BeregnetEtteroppgjoerResultatDto,
  EtteroppgjoerForbehandling,
  DetaljertEtteroppgjoerForbehandling,
} from '~shared/types/EtteroppgjoerForbehandling'
import { OppgaveDTO } from '~shared/types/oppgave'
import { useAppSelector } from '~store/Store'
import { IBrev } from '~shared/types/Brev'

export const addDetaljertEtteroppgjoerForbehandling =
  createAction<DetaljertEtteroppgjoerForbehandling>('etteroppgjoer/add')
export const addEtteroppgjoerOppgave = createAction<OppgaveDTO>('etteroppgjoer/oppgave/add')
export const addEtteroppgjoerBrev = createAction<IBrev>('etteroppgjoer/brev/add')
export const resetEtteroppgjoer = createAction('etteroppgjoer/reset')
export const addBeregnetEtteroppgjoerResultat =
  createAction<BeregnetEtteroppgjoerResultatDto>('etteroppgjoer/resultat/add')
export const updateEtteroppgjoerForbehandling = createAction<EtteroppgjoerForbehandling>(
  'etteroppgjoer/behandling/update'
)
const initialState: {
  etteroppgjoerForbehandling: DetaljertEtteroppgjoerForbehandling | null
  oppgave: OppgaveDTO | null
} = {
  etteroppgjoerForbehandling: null,
  oppgave: null,
}

export const etteroppgjoerReducer = createReducer(initialState, (builder) => {
  builder.addCase(addDetaljertEtteroppgjoerForbehandling, (state, action) => {
    state.etteroppgjoerForbehandling = action.payload
  })
  builder.addCase(addEtteroppgjoerBrev, (state, action) => {
    if (state.etteroppgjoerForbehandling?.forbehandling) {
      state.etteroppgjoerForbehandling.forbehandling.brevId = action.payload.id
    }
  })
  builder.addCase(updateEtteroppgjoerForbehandling, (state, action) => {
    if (state.etteroppgjoerForbehandling?.forbehandling) {
      state.etteroppgjoerForbehandling.forbehandling = action.payload
    }
  })
  builder.addCase(addEtteroppgjoerOppgave, (state, action) => {
    state.oppgave = action.payload
  })
  builder.addCase(resetEtteroppgjoer, (state) => {
    state.etteroppgjoerForbehandling = null
    state.oppgave = null
  })
  builder.addCase(addBeregnetEtteroppgjoerResultat, (state, action) => {
    state.etteroppgjoerForbehandling!.beregnetEtteroppgjoerResultat = action.payload
  })
})

export function useEtteroppgjoerForbehandling() {
  return useAppSelector((state) => state.etteroppgjoerReducer.etteroppgjoerForbehandling!)
}
