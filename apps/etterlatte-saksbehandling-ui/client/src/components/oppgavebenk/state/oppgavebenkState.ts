import { OppgaveDTO } from '~shared/types/oppgave'
import { GosysOppgave } from '~shared/types/Gosys'

export interface OppgavebenkState {
  oppgavelistaOppgaver: OppgaveDTO[]
  minOppgavelisteOppgaver: OppgaveDTO[]
  gosysOppgavelisteOppgaver: GosysOppgave[]
  oppgpavebenkStats: OppgavebenkStats
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
  oppgpavebenkStats: initalOppgavebenkStats,
}
