import { Oppgavestatus, Oppgavetype, Saktype } from '~shared/api/oppgaverny'
import { Button } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'
import { GosysOppgaveModal } from '~components/nyoppgavebenk/GosysOppgaveModal'

export const HandlingerForOppgave = (props: {
  oppgavetype: Oppgavetype
  oppgavestatus: Oppgavestatus
  opprettet: string
  frist: string
  fnr: string
  enhet: string
  saksbehandler: string | null
  saktype: Saktype
  referanse: string | null
  beskrivelse: string | null
  gjelder: string | null
}) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const {
    oppgavetype,
    oppgavestatus,
    opprettet,
    frist,
    fnr,
    enhet,
    saksbehandler,
    saktype,
    referanse,
    beskrivelse,
    gjelder,
  } = props
  const navigate = useNavigate()
  const erInnloggetSaksbehandlerOppgave = saksbehandler ? saksbehandler === user.ident : false

  const handling = () => {
    switch (oppgavetype) {
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
        return (
          <GosysOppgaveModal
            oppgavestatus={oppgavestatus}
            gjelder={gjelder}
            saktype={saktype}
            regdato={opprettet}
            fristdato={frist}
            enhet={enhet}
            saksbehandler={saksbehandler}
            fnr={fnr}
            beskrivelse={beskrivelse}
          />
        )
      default:
        return null
    }
  }

  return <>{handling()}</>
}
