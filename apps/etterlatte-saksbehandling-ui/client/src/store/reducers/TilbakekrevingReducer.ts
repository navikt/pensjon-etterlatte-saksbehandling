import { createAction, createReducer } from '@reduxjs/toolkit'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'

export const addTilbakekreving = createAction<Tilbakekreving>('tilbakekreving/add')
export const resetTilbakekreving = createAction('tilbakekreving/reset')

const initialState: { tilbakekreving: Tilbakekreving | null } = {
  tilbakekreving: null,
}
export const tilbakekrevingReducer = createReducer(initialState, (builder) => {
  builder.addCase(addTilbakekreving, (state, action) => {
    state.tilbakekreving = action.payload
  })
  builder.addCase(resetTilbakekreving, (state) => {
    state.tilbakekreving = null
  })
})
