import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'
import { createAction, createReducer } from '@reduxjs/toolkit'

export enum Valideringsfeilkoder {
  MAA_HUKES_AV = 'Feltet må hukes av for å ferdigstilles',
}

export const updateSjekkliste = createAction<ISjekkliste>('sjekkliste/update')
export const updateSjekklisteItem = createAction<ISjekklisteItem>('sjekkliste/updateItem')
export const addValideringsfeil = createAction<Valideringsfeilkoder>('sjekkliste/addValideringsfeil')

const initialState: { sjekkliste: ISjekkliste | null; valideringsfeil: Valideringsfeilkoder[] } = {
  sjekkliste: null,
  valideringsfeil: [],
}

export const sjekklisteReducer = createReducer(initialState, (builder) => {
  builder.addCase(updateSjekkliste, (state, action) => {
    state.sjekkliste = action.payload
    state.valideringsfeil = []
  })
  builder.addCase(updateSjekklisteItem, (state, action) => {
    const oppdatertItem = action.payload
    if (state.sjekkliste) {
      const updatedList = state.sjekkliste.sjekklisteItems.map((it) =>
        it.id === oppdatertItem.id ? oppdatertItem : it
      )
      state.sjekkliste.sjekklisteItems = updatedList
    }
  })
  builder.addCase(addValideringsfeil, (state, action) => {
    state.valideringsfeil.push(action.payload)
  })
})
