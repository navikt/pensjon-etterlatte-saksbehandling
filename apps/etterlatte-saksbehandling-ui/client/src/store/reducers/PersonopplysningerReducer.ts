import { createAction, createReducer } from '@reduxjs/toolkit'
import { Personopplysninger } from '~shared/types/grunnlag'

export const setPersonopplysninger = createAction<Personopplysninger>('behandlingPersoner/set')
export const resetPersonopplysninger = createAction('behandlingPersoner/reset')

const initialState: { personopplysninger: Personopplysninger | null } = {
  personopplysninger: null,
}

export const personopplysningerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setPersonopplysninger, (state, action) => {
    state.personopplysninger = action.payload
  })
  builder.addCase(resetPersonopplysninger, (state) => {
    state.personopplysninger = null
  })
})
