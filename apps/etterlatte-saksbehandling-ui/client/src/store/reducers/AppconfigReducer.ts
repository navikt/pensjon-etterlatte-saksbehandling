import { createAction, createReducer } from '@reduxjs/toolkit'

const initialState: { appversion: null | string } = {
  appversion: null,
}

export const settAppversion = createAction<string>('appversion')

export const appReducer = createReducer(initialState, (builder) => {
  builder.addCase(settAppversion, (state, action) => {
    state.appversion = action.payload
  })
})
