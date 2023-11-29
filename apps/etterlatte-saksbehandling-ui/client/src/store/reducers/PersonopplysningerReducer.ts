import { createAction, createReducer } from '@reduxjs/toolkit'
import { Personopplysning, Personopplysninger } from '~shared/types/grunnlag'

export const setPersonopplysninger = createAction<Personopplysninger>('behandlingPersoner/set')
export const resetPersonopplysninger = createAction('behandlingPersoner/reset')

const initialState: {
  personopplysninger: Personopplysninger | null
  avdoede: Personopplysning | null
  gjenlevende: Personopplysning | null
} = {
  personopplysninger: null,
  avdoede: null,
  gjenlevende: null,
}

export const personopplysningerReducer = createReducer(initialState, (builder) => {
  builder.addCase(setPersonopplysninger, (state, action) => {
    state.personopplysninger = action.payload
    state.avdoede = action.payload.avdoede.find((po) => po) ?? null
    state.gjenlevende = action.payload.gjenlevende.find((po) => po) ?? null
  })
  builder.addCase(resetPersonopplysninger, (state) => {
    state.personopplysninger = null
    state.avdoede = null
    state.gjenlevende = null
  })
})
