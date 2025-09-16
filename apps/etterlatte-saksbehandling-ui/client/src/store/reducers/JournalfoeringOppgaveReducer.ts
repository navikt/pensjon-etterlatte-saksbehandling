import { createAction, createReducer } from '@reduxjs/toolkit'
import { Journalpost } from '~shared/types/Journalpost'
import { SakMedBehandlingerOgKanskjeAnnenSak } from '~components/person/typer'
import { NyKlageRequestUtfylling } from '~shared/types/Klage'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { OppgaveDTO } from '~shared/types/oppgave'

export const settBruker = createAction<string>('behandling/bruker/sett')
export const settOppgave = createAction<OppgaveDTO>('behandling/oppgave/sett')
export const settSak = createAction<SakMedBehandlingerOgKanskjeAnnenSak>('behandling/sak/sett')
export const settJournalpost = createAction<Journalpost>('behandling/journalpost/sett')
export const settOppgaveHandling = createAction<OppgaveHandling>('oppgave/oppgavehandling/sett')
export const settNyBehandlingRequest = createAction<NyBehandlingRequest>('behandling/behandlingbehov/sett')
export const settNyKlageRequest = createAction<Partial<NyKlageRequestUtfylling>>('klage/klagebehandling/sett')

export interface IJournalfoeringOppgaveReducer {
  bruker?: string
  oppgave?: OppgaveDTO
  sakMedBehandlinger?: SakMedBehandlingerOgKanskjeAnnenSak
  journalpost?: Journalpost
  oppgaveHandling?: OppgaveHandling
  nyBehandlingRequest?: NyBehandlingRequest
  nyKlageRequest?: Partial<NyKlageRequestUtfylling>
}

export enum OppgaveHandling {
  NY_BEHANDLING = 'NY_BEHANDLING',
  FERDIGSTILL_OPPGAVE = 'FERDIGSTILL_OPPGAVE',
  NY_KLAGE = 'NY_KLAGE',
  SVAR_ETTEROPPGJOER = 'SVAR_ETTEROPPGJOER',
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
    .addCase(settNyKlageRequest, (state, action) => {
      state.nyKlageRequest = action.payload
    })
)
