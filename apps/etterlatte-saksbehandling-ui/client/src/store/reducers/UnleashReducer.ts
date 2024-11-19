import { createAction, createReducer } from '@reduxjs/toolkit'
import { Toggle, FeatureToggle, unleashStartState } from '~useUnleash'
import { useAppSelector } from '~store/Store'

export const endreToggle = createAction<Toggle>('sett/Toggle')

export const unleashReducer = createReducer(unleashStartState, (builder) => {
  builder.addCase(endreToggle, (state, action) => {
    state[action.payload.togglename] = {
      ...state[action.payload.togglename],
      enabled: action.payload.enabled,
    }
  })
})

export function useUnleashReducerToggle(featureToggle: FeatureToggle): Toggle {
  return useAppSelector((state) => state.unleashReducer[featureToggle])
}
export function useUnleashReducer(): Record<string, Toggle> {
  return useAppSelector((state) => state.unleashReducer)
}
