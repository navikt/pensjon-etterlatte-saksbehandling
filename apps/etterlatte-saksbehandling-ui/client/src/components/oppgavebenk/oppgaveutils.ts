import { OppgaveDTO } from '~shared/api/oppgaver'

export const oppdaterTildeling =
  (setHentedeOppgaver: (oppdatertListe: OppgaveDTO[]) => void, hentedeOppgaver: OppgaveDTO[]) =>
  (id: string, saksbehandler: string | null, versjon: number | null) => {
    setTimeout(() => {
      const oppdatertOppgaveState = [...hentedeOppgaver]
      const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
      oppdatertOppgaveState[index].saksbehandlerIdent = saksbehandler
      oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
      oppdatertOppgaveState[index].versjon = versjon
      setHentedeOppgaver(oppdatertOppgaveState)
    }, 2000)
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
