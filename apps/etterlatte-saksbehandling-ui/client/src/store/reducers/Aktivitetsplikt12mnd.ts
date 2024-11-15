import { createAction, createReducer } from '@reduxjs/toolkit'
import { AktivitetspliktOppgaveVurdering, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { useAppSelector } from '~store/Store'
import { IBrevAktivitetspliktDto } from '~shared/api/aktivitetsplikt'
import { OppgaveDTO } from '~shared/types/oppgave'

//Reduceren er kun klar til å brukes etter at setStartdata er kalt
const initialState: AktivitetspliktOppgaveVurdering = {} as AktivitetspliktOppgaveVurdering

export const setStartdata = createAction<AktivitetspliktOppgaveVurdering>('set/AktivitetspliktOppgaveVurdering')
export const setBrevid = createAction<number>('set/AktivitetspliktOppgaveVurdering/aktivtetspliktbrevdata/brevid')
export const setAktivtetspliktbrevdata = createAction<IBrevAktivitetspliktDto>(
  'set/AktivitetspliktOppgaveVurdering/aktivtetspliktbrevdata'
)
export const setAktivitetspliktVurdering = createAction<IAktivitetspliktVurderingNyDto>(
  'set/AktivitetspliktOppgaveVurdering/aktivitetspliktVurdering'
)
export const setAktivitetspliktOppgave = createAction<OppgaveDTO>('set/AktivitetspliktOppgaveVurdering/oppgave')

export const aktivitetsplikt12mndReducer = createReducer(initialState, (builder) => {
  builder.addCase(setStartdata, (_, action) => action.payload)
  builder.addCase(setAktivtetspliktbrevdata, (state, action) => {
    if (state) {
      state.aktivtetspliktbrevdata = action.payload
    }
  })
  builder.addCase(setBrevid, (state, action) => {
    if (state.aktivtetspliktbrevdata) {
      state.aktivtetspliktbrevdata.brevId = action.payload
    }
  })
  builder.addCase(setAktivitetspliktVurdering, (state, action) => {
    if (state) {
      state.vurdering = action.payload
    }
  })
  builder.addCase(setAktivitetspliktOppgave, (state, action) => {
    if (state) {
      state.oppgave = action.payload
    }
  })
})

export function useAktivitetspliktOppgaveVurderingState(): AktivitetspliktOppgaveVurdering {
  return useAppSelector((state) => state.aktivitetsplikt12mndReducer)
}
