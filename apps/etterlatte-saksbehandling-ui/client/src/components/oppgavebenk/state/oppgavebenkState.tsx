import { OppgaveDTO } from '~shared/api/oppgaver'

export interface OppgavebenkState {
  oppgavelistaOppgaver: OppgaveDTO[]
  minOppgavelisteOppgaver: OppgaveDTO[]
  gosysOppgavelisteOppgaver: OppgaveDTO[]
}

export const initialOppgavebenkState: OppgavebenkState = {
  oppgavelistaOppgaver: [],
  minOppgavelisteOppgaver: [],
  gosysOppgavelisteOppgaver: [],
}
