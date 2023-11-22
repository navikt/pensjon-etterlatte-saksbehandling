import { useAppSelector } from '~store/Store'
import { mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { Alert, Button, Loader } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'

export const TildelSaksbehandler = (props: {
  oppgaveId: string
  oppdaterTildeling: (id: string, saksbehandler: string) => void
  erRedigerbar: boolean
  versjon: string | null
  type: Oppgavetype
}) => {
  const { oppgaveId, oppdaterTildeling, erRedigerbar, versjon, type } = props
  if (!erRedigerbar) {
    return null
  }

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [tildelSaksbehandlerSvar, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const tildelSaksbehandlerWrapper = () => {
    const nysaksbehandler = { saksbehandler: innloggetSaksbehandler.ident, versjon }

    tildelSaksbehandler({ oppgaveId, type, nysaksbehandler }, () => {
      oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident)
    })
  }

  return mapAllApiResult(
    tildelSaksbehandlerSvar,
    <Loader size="small" title="Setter saksbehandler" />,
    <Button icon={<PersonIcon />} variant="tertiary" size="small" onClick={tildelSaksbehandlerWrapper}>
      Tildel meg
    </Button>,
    () => (
      <Alert variant="error" size="small">
        Tildeling feilet
      </Alert>
    ),
    () => (
      <Alert variant="success" size="small">
        Oppgave tildelt deg
      </Alert>
    )
  )
}
