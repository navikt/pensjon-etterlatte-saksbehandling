import { createAction, createReducer } from '@reduxjs/toolkit'

const initialState: { hovedlista: number; minliste: number } = {
  hovedlista: 0,
  minliste: 0,
}

export const settHovedOppgavelisteLengde = createAction<number>('oppgavelistelengde')

export const settMinOppgavelisteLengde = createAction<number>('minoppgavelistelende')

export const oppgaveRedurcer = createReducer(initialState, (builder) => {
  builder.addCase(settHovedOppgavelisteLengde, (state, action) => {
    state.hovedlista = action.payload
  })
  builder.addCase(settMinOppgavelisteLengde, (state, action) => {
    state.minliste = action.payload
  })
})
