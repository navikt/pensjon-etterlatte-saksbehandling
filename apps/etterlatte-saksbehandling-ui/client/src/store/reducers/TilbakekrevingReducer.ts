import { createAction, createReducer } from '@reduxjs/toolkit'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'

export const addTilbakekreving = createAction<TilbakekrevingBehandling>('tilbakekreving/add')
export const resetTilbakekreving = createAction('tilbakekreving/reset')

const initialState: { tilbakekrevingBehandling: TilbakekrevingBehandling | null } = {
  tilbakekrevingBehandling: null,
}

export const tilbakekrevingReducer = createReducer(initialState, (builder) => {
  builder.addCase(addTilbakekreving, (state, action) => {
    state.tilbakekrevingBehandling = action.payload
  })
  builder.addCase(resetTilbakekreving, (state) => {
    state.tilbakekrevingBehandling = null
  })
})
