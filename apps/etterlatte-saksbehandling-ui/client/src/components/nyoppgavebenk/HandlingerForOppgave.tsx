import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { Button } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'
import { GosysOppgaveModal } from '~components/nyoppgavebenk/GosysOppgaveModal'

export const HandlingerForOppgave = ({ oppgave }: { oppgave: OppgaveDTOny }) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const navigate = useNavigate()

  const { type, fnr, saksbehandler, referanse } = oppgave
  const erInnloggetSaksbehandlerOppgave = saksbehandler ? saksbehandler === user.ident : false

  const handling = () => {
    switch (type) {
      case 'VURDER_KONSEKVENS':
        return (
          <>
            <Button size="small" icon={<EyeIcon />} onClick={() => navigate(`/person/${fnr}`)}>
              Se hendelse
            </Button>
          </>
        )
      case 'UNDERKJENT':
      case 'FOERSTEGANGSBEHANDLING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" onClick={() => navigate(`/behandling/${referanse}`)}>
                Gå til behandling
              </Button>
            )}
          </>
        )
      case 'REVURDERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" onClick={() => navigate(`/behandling/${referanse}`)}>
                Gå til revurdering
              </Button>
            )}
          </>
        )
      case 'MANUELT_OPPHOER':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" onClick={() => navigate(`/behandling/${referanse}`)}>
                Gå til opphør
              </Button>
            )}
          </>
        )
      case 'ATTESTERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" onClick={() => navigate(`/behandling/${referanse}`)}>
                Gå til attestering
              </Button>
            )}
          </>
        )
      case 'GOSYS':
        return <GosysOppgaveModal oppgave={oppgave} />
      default:
        return null
    }
  }

  return <>{handling()}</>
}
