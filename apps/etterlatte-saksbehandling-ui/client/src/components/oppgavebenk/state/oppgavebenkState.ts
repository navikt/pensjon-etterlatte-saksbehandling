import { OppgaveDTO } from '~shared/api/oppgaver'

export interface OppgavebenkState {
  oppgavelistaOppgaver: OppgaveDTO[]
  minOppgavelisteOppgaver: OppgaveDTO[]
  gosysOppgavelisteOppgaver: OppgaveDTO[]
  opgpavebenkStats: OppgavebenkStats
}
export interface OppgavebenkStats {
  antallOppgavelistaOppgaver: number
  antallMinOppgavelisteOppgaver: number
}

export const initalOppgavebenkStats: OppgavebenkStats = {
  antallOppgavelistaOppgaver: 0,
  antallMinOppgavelisteOppgaver: 0,
}

export const initialOppgavebenkState: OppgavebenkState = {
  oppgavelistaOppgaver: [],
  minOppgavelisteOppgaver: [],
  gosysOppgavelisteOppgaver: [],
  opgpavebenkStats: initalOppgavebenkStats,
}
