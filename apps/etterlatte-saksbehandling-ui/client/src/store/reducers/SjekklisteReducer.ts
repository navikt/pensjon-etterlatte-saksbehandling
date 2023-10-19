import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'
import { createAction, createReducer } from '@reduxjs/toolkit'

export const updateSjekkliste = createAction<ISjekkliste>('sjekkliste/update')
export const updateSjekklisteItem = createAction<ISjekklisteItem>('sjekkliste/updateItem')
export const addValideringsfeil = createAction<String>('sjekkliste/addValideringsfeil')
export const resetValideringsfeil = createAction('sjekkliste/resetValidering')

const initialState: { sjekkliste: ISjekkliste | null; valideringsfeil: String[] } = {
  sjekkliste: null,
  valideringsfeil: [],
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
  builder.addCase(addValideringsfeil, (state, action) => {
    state.valideringsfeil = [action.payload]
  })
  builder.addCase(resetValideringsfeil, (state) => {
    state.valideringsfeil = []
  })
})
