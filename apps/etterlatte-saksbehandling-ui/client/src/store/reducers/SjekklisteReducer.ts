import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'
import { createAction, createReducer } from '@reduxjs/toolkit'

export const updateSjekkliste = createAction<ISjekkliste>('sjekkliste/update')
export const updateSjekklisteItem = createAction<ISjekklisteItem>('sjekkliste/updateItem')

const initialState: { sjekkliste: ISjekkliste | null } = {
  sjekkliste: null,
}

export const sjekklisteReducer = createReducer(initialState, (builder) => {
  builder.addCase(updateSjekkliste, (state, action) => {
    state.sjekkliste = action.payload
  })
  builder.addCase(updateSjekklisteItem, (state, action) => {
    const oppdatertItem = action.payload
    const updatedList = state.sjekkliste!.sjekklisteItems.map((it) => (it.id === oppdatertItem.id ? oppdatertItem : it))
    state.sjekkliste = {
      ...state.sjekkliste!,
      sjekklisteItems: updatedList,
    }
  })
})
