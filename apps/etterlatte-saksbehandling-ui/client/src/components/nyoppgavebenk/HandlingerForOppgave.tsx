import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { Button } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'
import { GosysOppgaveModal } from '~components/nyoppgavebenk/GosysOppgaveModal'

export const HandlingerForOppgave = ({ oppgave }: { oppgave: OppgaveDTOny }) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const { type, fnr, saksbehandler, referanse } = oppgave
  const erInnloggetSaksbehandlerOppgave = saksbehandler ? saksbehandler === user.ident : false

  const handling = () => {
    switch (type) {
      case 'VURDER_KONSEKVENS':
        return (
          <>
            <Button size="small" icon={<EyeIcon />} href={`/person/${fnr}`} as="a">
              Se hendelse
            </Button>
          </>
        )
      case 'UNDERKJENT':
      case 'FOERSTEGANGSBEHANDLING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" as="a" href={`/behandling/${referanse}`}>
                Gå til behandling
              </Button>
            )}
          </>
        )
      case 'REVURDERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" href={`/behandling/${referanse}`} as="a">
                Gå til revurdering
              </Button>
            )}
          </>
        )
      case 'MANUELT_OPPHOER':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" href={`/behandling/${referanse}`} as="a">
                Gå til opphør
              </Button>
            )}
          </>
        )
      case 'ATTESTERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" href={`/behandling/${referanse}`} as="a">
                Gå til attestering
              </Button>
            )}
          </>
        )
      case 'GOSYS':
        return <GosysOppgaveModal oppgave={oppgave} />
      case 'KLAGE':
        return erInnloggetSaksbehandlerOppgave ? (
          <Button size="small" href={`/klage/${referanse}`} as="a">
            Gå til klage
          </Button>
        ) : null
      default:
        return null
    }
  }

  return <>{handling()}</>
}
