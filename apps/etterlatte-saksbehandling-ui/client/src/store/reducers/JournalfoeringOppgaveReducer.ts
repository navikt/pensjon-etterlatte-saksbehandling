import { createAction, createReducer } from '@reduxjs/toolkit'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { JaNei } from '~shared/types/ISvar'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { Journalpost } from '~shared/types/Journalpost'

export const settBruker = createAction<string>('behandling/bruker/sett')
export const settOppgave = createAction<OppgaveDTOny>('behandling/oppgave/sett')
export const settJournalpost = createAction<Journalpost>('behandling/journalpost/sett')
export const settSamsvar = createAction<JaNei>('behandling/samsvar/sett')
export const settBehandlingBehov = createAction<NyBehandlingRequest>('behandling/behandlingbehov/sett')

export interface IJournalfoeringOppgaveReducer {
  bruker?: string
  oppgave?: OppgaveDTOny
  journalpost?: Journalpost
  samsvar?: JaNei
  behandlingBehov?: NyBehandlingRequest
}

const initialState: IJournalfoeringOppgaveReducer = {}

export const journalfoeringOppgaveReducer = createReducer(initialState, (builder) =>
  builder
    .addCase(settBruker, (state, action) => {
      state.bruker = action.payload
    })
    .addCase(settOppgave, (state, action) => {
      state.oppgave = action.payload
    })
    .addCase(settJournalpost, (state, action) => {
      state.journalpost = action.payload
    })
    .addCase(settSamsvar, (state, action) => {
      state.samsvar = action.payload
    })
    .addCase(settBehandlingBehov, (state, action) => {
      state.behandlingBehov = action.payload
    })
)
