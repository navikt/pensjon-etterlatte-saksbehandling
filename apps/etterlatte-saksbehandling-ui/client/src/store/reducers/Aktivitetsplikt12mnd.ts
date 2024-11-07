import { createAction, createReducer } from '@reduxjs/toolkit'
import { AktivitetspliktOppgaveVurdering, IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
import { useAppSelector } from '~store/Store'
import { IBrevAktivitetspliktDto } from '~shared/api/aktivitetsplikt'

const initialState: AktivitetspliktOppgaveVurdering = {} as AktivitetspliktOppgaveVurdering

export const setStartdata = createAction<AktivitetspliktOppgaveVurdering>('set/AktivitetspliktOppgaveVurdering')
export const setBrevid = createAction<number>('set/AktivitetspliktOppgaveVurdering/aktivtetspliktbrevdata/brevid')
export const setAktivtetspliktbrevdata = createAction<IBrevAktivitetspliktDto>(
  'set/AktivitetspliktOppgaveVurdering/aktivtetspliktbrevdata'
)
export const setAktivitetspliktVurdering = createAction<IAktivitetspliktVurderingNy>(
  'set/AktivitetspliktOppgaveVurdering/aktivitetspliktVurdering'
)

export const Aktivitetsplikt12mndReducer = createReducer(initialState, (builder) => {
  builder.addCase(setStartdata, (_, action) => action.payload)
  builder.addCase(setAktivtetspliktbrevdata, (state, action) => {
    if (state.aktivtetspliktbrevdata) {
      state.aktivtetspliktbrevdata = action.payload
    }
  })
  builder.addCase(setBrevid, (state, action) => {
    if (state.aktivtetspliktbrevdata) {
      state.aktivtetspliktbrevdata.brevId = action.payload
    }
  })
  builder.addCase(setAktivitetspliktVurdering, (state, action) => {
    if (state.vurdering) {
      state.vurdering = action.payload
    }
  })
})

export function useAktivitetspliktOppgaveVurderingState(): AktivitetspliktOppgaveVurdering {
  return useAppSelector((state) => state.Aktivitetsplikt12mndReducer)
}
