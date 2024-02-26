import { createAction, createReducer } from '@reduxjs/toolkit'
import { erKlageRedigerbar, Klage } from '~/shared/types/Klage'

export const addKlage = createAction<Klage>('klage/add')
export const resetKlage = createAction('klage/reset')

const initialState: { klage: Klage | null; redigerbar: boolean } = {
  klage: null,
  redigerbar: true,
}
export const klageReducer = createReducer(initialState, (builder) => {
  builder.addCase(addKlage, (state, action) => {
    state.klage = action.payload
    state.redigerbar = erKlageRedigerbar(action.payload)
  })
  builder.addCase(resetKlage, (state) => {
    state.klage = null
    state.redigerbar = true
  })
})
