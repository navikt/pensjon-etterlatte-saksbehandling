import { createAction, createReducer } from '@reduxjs/toolkit'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import {
  FerdigstillJournalpostRequest,
  Journalpost,
  JournalpostVariant,
  OppdaterJournalpostTemaRequest,
} from '~shared/types/Journalpost'

export const settBruker = createAction<string>('behandling/bruker/sett')
export const settOppgave = createAction<OppgaveDTO>('behandling/oppgave/sett')
export const settJournalpost = createAction<Journalpost>('behandling/journalpost/sett')
export const settJournalpostVariant = createAction<JournalpostVariant>('oppgave/journalpostvariant/sett')
export const settNyttTema = createAction<OppdaterJournalpostTemaRequest>('oppgave/endretemarequest/sett')
export const settFerdigstillRequest = createAction<FerdigstillJournalpostRequest>('oppgave/ferdigstillrequest/sett')
export const settNyBehandlingRequest = createAction<NyBehandlingRequest>('behandling/behandlingbehov/sett')

export interface IJournalfoeringOppgaveReducer {
  bruker?: string
  oppgave?: OppgaveDTO
  journalpost?: Journalpost
  journalpostVariant?: JournalpostVariant
  endreTemaRequest?: OppdaterJournalpostTemaRequest
  ferdigstillRequest?: FerdigstillJournalpostRequest
  nyBehandlingRequest?: NyBehandlingRequest
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
    .addCase(settJournalpostVariant, (state, action) => {
      state.journalpostVariant = action.payload
    })
    .addCase(settNyttTema, (state, action) => {
      state.endreTemaRequest = action.payload
    })
    .addCase(settFerdigstillRequest, (state, action) => {
      state.ferdigstillRequest = action.payload
    })
    .addCase(settNyBehandlingRequest, (state, action) => {
      state.nyBehandlingRequest = action.payload
    })
)
