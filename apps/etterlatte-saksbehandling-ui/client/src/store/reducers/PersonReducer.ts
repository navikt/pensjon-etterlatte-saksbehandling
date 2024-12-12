import { createAction, createReducer } from '@reduxjs/toolkit'
import { IPdlPersonNavnFoedsel } from '~shared/types/Person'

export const settPerson = createAction<IPdlPersonNavnFoedsel | null>('person/set')
export const resetPerson = createAction('person/reset')

const initialState: { person: IPdlPersonNavnFoedsel | null } = {
  person: null,
}

export const personReducer = createReducer(initialState, (builder) => {
  builder.addCase(settPerson, (state, action) => {
    state.person = action.payload
  })
  builder.addCase(resetPerson, (state) => {
    state.person = null
  })
})
