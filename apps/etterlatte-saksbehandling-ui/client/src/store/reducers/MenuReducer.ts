import { createAction, createReducer } from '@reduxjs/toolkit'

export const toggle = createAction('menu/toggle')

export interface IMenuReducer {
  open: boolean
}
const initialState: IMenuReducer = { open: false }

export const menuReducer = createReducer(initialState, (builder) => {
  builder.addCase(toggle, (state) => {
    state.open = !state.open
  })
})
