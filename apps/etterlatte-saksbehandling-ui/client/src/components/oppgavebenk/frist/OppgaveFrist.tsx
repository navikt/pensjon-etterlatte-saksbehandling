import { erOppgaveRedigerbar, erOppgaveTildeltInnloggetSaksbehandler, OppgaveDTO } from '~shared/types/oppgave'
import { SettNyOppgaveFristModal } from '~components/oppgavebenk/frist/SettNyOppgaveFristModal'
import { StatusPaaOppgaveFrist } from '~components/oppgavebenk/frist/StatusPaaOppgaveFrist'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

interface Props {
  oppgave: OppgaveDTO
  oppdaterFrist: (oppgaveId: string, nyFrist: string) => void
}

export const OppgaveFrist = ({ oppgave, oppdaterFrist }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  return erOppgaveRedigerbar(oppgave.status) &&
    erOppgaveTildeltInnloggetSaksbehandler(oppgave, innloggetSaksbehandler) ? (
    <SettNyOppgaveFristModal oppgave={oppgave} oppdaterFrist={oppdaterFrist} />
  ) : (
    <StatusPaaOppgaveFrist oppgave={oppgave} />
  )
}
