import { createAction, createReducer } from '@reduxjs/toolkit'
import { ISak } from '~shared/types/sak'

export const settSak = createAction<ISak | null>('sak/set')
export const resetSak = createAction('sak/reset')

const initialState: { sak: ISak | null } = {
  sak: null,
}

export const sakReducer = createReducer(initialState, (builder) => {
  builder.addCase(settSak, (state, action) => {
    state.sak = action.payload
  })
  builder.addCase(resetSak, (state) => {
    state.sak = null
  })
})
