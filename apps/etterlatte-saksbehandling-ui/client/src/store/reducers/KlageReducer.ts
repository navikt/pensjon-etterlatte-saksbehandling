import { createAction, createReducer } from '@reduxjs/toolkit'
import { Klage } from '~/shared/types/Klage'

export const addKlage = createAction<Klage>('klage/add')
export const resetKlage = createAction('klage/reset')

const initialState: { klage: Klage | null } = {
  klage: null,
}
export const klageReducer = createReducer(initialState, (builder) => {
  builder.addCase(addKlage, (state, action) => {
    state.klage = action.payload
  })
  builder.addCase(resetKlage, (state) => {
    state.klage = null
  })
})
