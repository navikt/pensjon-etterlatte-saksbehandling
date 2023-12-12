import { createAction, createReducer } from '@reduxjs/toolkit'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { Journalpost } from '~shared/types/Journalpost'
import { SakMedBehandlinger } from '~components/person/typer'

export const settBruker = createAction<string>('behandling/bruker/sett')
export const settOppgave = createAction<OppgaveDTO>('behandling/oppgave/sett')
export const settSak = createAction<SakMedBehandlinger>('behandling/sak/sett')
export const settJournalpost = createAction<Journalpost>('behandling/journalpost/sett')
export const settOppgaveHandling = createAction<OppgaveHandling>('oppgave/oppgavehandling/sett')
export const settNyBehandlingRequest = createAction<NyBehandlingRequest>('behandling/behandlingbehov/sett')

export interface IJournalfoeringOppgaveReducer {
  bruker?: string
  oppgave?: OppgaveDTO
  sakMedBehandlinger?: SakMedBehandlinger
  journalpost?: Journalpost
  oppgaveHandling?: OppgaveHandling
  nyBehandlingRequest?: NyBehandlingRequest
}

export enum OppgaveHandling {
  NY_BEHANDLING = 'NY_BEHANDLING',
  FERDIGSTILL_OPPGAVE = 'FERDIGSTILL_OPPGAVE',
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
    .addCase(settSak, (state, action) => {
      state.sakMedBehandlinger = action.payload
    })
    .addCase(settJournalpost, (state, action) => {
      state.journalpost = action.payload
    })
    .addCase(settOppgaveHandling, (state, action) => {
      state.oppgaveHandling = action.payload
    })
    .addCase(settNyBehandlingRequest, (state, action) => {
      state.nyBehandlingRequest = action.payload
    })
)
