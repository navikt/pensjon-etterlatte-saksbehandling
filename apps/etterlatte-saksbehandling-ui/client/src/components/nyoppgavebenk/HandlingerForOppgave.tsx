import { Oppgavetype } from '~shared/api/oppgaverny'
import { Button } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'

export const HandlingerForOppgave = (props: {
  oppgavetype: Oppgavetype
  fnr: string
  saksbehandler: string | null
  referanse: string | null
}) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const { oppgavetype, fnr, saksbehandler, referanse } = props
  const navigate = useNavigate()
  const erInnloggetSaksbehandlerOppgave = saksbehandler ? saksbehandler === user.ident : false

  const handling = () => {
    switch (oppgavetype) {
      case 'HENDELSE':
        return (
          <>
            <Button icon={<EyeIcon />} onClick={() => navigate(`/person/${fnr}`)}>
              Se hendelse
            </Button>
          </>
        )
      case 'FOERSTEGANGSBEHANDLING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button onClick={() => navigate(`/behandling/${referanse}`)}>Gå til behandling</Button>
            )}
          </>
        )
      case 'REVURDERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button onClick={() => navigate(`/behandling/${referanse}`)}>Gå til revurdering</Button>
            )}
          </>
        )
      case 'MANUELT_OPPHOER':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button onClick={() => navigate(`/behandling/${referanse}`)}>Gå til opphør</Button>
            )}
          </>
        )
      case 'ATTESTERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button onClick={() => navigate(`/behandling/${referanse}`)}>Gå til attestering</Button>
            )}
          </>
        )
      default:
        return null
    }
  }

  return <>{handling()}</>
}
