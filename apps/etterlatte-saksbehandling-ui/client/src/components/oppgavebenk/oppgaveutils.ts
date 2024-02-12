import { OppgaveDTO } from '~shared/api/oppgaver'

export const finnOgOppdaterSaksbehandlerTildeling = (
  oppgaver: OppgaveDTO[],
  oppgaveId: string,
  saksbehandler: string | null,
  versjon: number | null
) => {
  const index = oppgaver.findIndex((o) => o.id === oppgaveId)
  if (index > -1) {
    const oppdatertOppgaveState = [...oppgaver]
    oppdatertOppgaveState[index].saksbehandlerIdent = saksbehandler
    oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
    oppdatertOppgaveState[index].versjon = versjon
    return oppdatertOppgaveState
  } else {
    return oppgaver
  }
}

export const oppdaterTildelingPaaEgenOppgave = (
  oppgaver: OppgaveDTO[],
  oppgave: OppgaveDTO,
  saksbehandler: string | null,
  versjon: number | null
): OppgaveDTO[] => {
  return [...oppgaver, { ...oppgave, saksbehandlerIdent: saksbehandler, status: 'UNDER_BEHANDLING', versjon: versjon }]
}

export const oppdaterFrist = (
  setHentedeOppgaver: (oppdatertListe: OppgaveDTO[]) => void,
  hentedeOppgaver: OppgaveDTO[],
  id: string,
  frist: string,
  versjon: number | null
) => {
  setTimeout(() => {
    const oppdatertOppgaveState = [...hentedeOppgaver]
    const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
    oppdatertOppgaveState[index].frist = frist
    oppdatertOppgaveState[index].versjon = versjon
    setHentedeOppgaver(oppdatertOppgaveState)
  }, 2000)
}

export const sorterOppgaverEtterOpprettet = (oppgaver: OppgaveDTO[]) => {
  return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
}
