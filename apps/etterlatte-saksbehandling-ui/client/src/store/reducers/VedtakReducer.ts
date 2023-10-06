import { createAction, createReducer } from '@reduxjs/toolkit'
import { VedtakSammendrag } from '~components/vedtak/typer'

export const updateVedtakSammendrag = createAction<VedtakSammendrag>('vedtak/update_vedtaksammendrag')

const initialState: { vedtak: VedtakSammendrag | null } = {
  vedtak: null,
}

export const vedtakReducer = createReducer(initialState, (builder) => {
  builder.addCase(updateVedtakSammendrag, (state, action) => {
    state.vedtak = action.payload
  })
})
