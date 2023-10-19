import { BehandlingFane } from '~components/behandling/sidemeny/IBehandlingInfo'
import { createAction, createReducer } from '@reduxjs/toolkit'

export const visDokumentoversikt = createAction('behandlingsidemeny/visDokumentoversikt')
export const visSjekkliste = createAction('behandlingsidemeny/visSjekkliste')
export const visFane = createAction<BehandlingFane>('behandlingsidemeny/visFane')

const initialState: { fane: BehandlingFane } = {
  fane: BehandlingFane.DOKUMENTER,
}

export const behandlingsidemenyReducer = createReducer(initialState, (builder) => {
  builder.addCase(visDokumentoversikt, (state) => {
    state.fane = BehandlingFane.DOKUMENTER
  })
  builder.addCase(visSjekkliste, (state) => {
    state.fane = BehandlingFane.SJEKKLISTE
  })
  builder.addCase(visFane, (state, action) => {
    state.fane = action.payload
  })
})
