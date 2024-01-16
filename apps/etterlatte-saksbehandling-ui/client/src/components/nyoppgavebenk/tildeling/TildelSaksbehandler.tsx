import { useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, tildelSaksbehandlerApi } from '~shared/api/oppgaver'
import { Alert, Button, Loader } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'

import { mapAllApiResult } from '~shared/api/apiUtils'

export const TildelSaksbehandler = (props: {
  oppgaveId: string
  oppdaterTildeling: (id: string, saksbehandler: string, versjon: number | null) => void
  erRedigerbar: boolean
  versjon: number | null
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

    tildelSaksbehandler({ oppgaveId, type, nysaksbehandler }, (result) => {
      oppdaterTildeling(oppgaveId, innloggetSaksbehandler.ident, result.versjon)
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
        Oppgaven er tildelt deg
      </Alert>
    )
  )
}
