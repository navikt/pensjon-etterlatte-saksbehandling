import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { tildelSaksbehandlerApi } from '~shared/api/oppgaverny'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Button, Loader } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'
import { Alert } from '@navikt/ds-react'

export const TildelSaksbehandler = (props: { oppgaveId: string; sakId: number }) => {
  const { oppgaveId, sakId } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [tildelSaksbehandlerSvar, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)
  const tildelSaksbehandlerWrapper = () => {
    tildelSaksbehandler({ nysaksbehandler: { oppgaveId: oppgaveId, saksbehandler: user.ident }, sakId: sakId })
  }
  return (
    <>
      {isPending(tildelSaksbehandlerSvar) && <Loader size="small" title="Setter saksbehandler" />}
      {isSuccess(tildelSaksbehandlerSvar) && <Alert variant="success">Oppgaven ble lagt på din oppgaveliste</Alert>}
      {isFailure(tildelSaksbehandlerSvar) && <ApiErrorAlert>Kunne ikke tildele deg denne oppgaven</ApiErrorAlert>}
      {isInitial(tildelSaksbehandlerSvar) && (
        <Button icon={<PersonIcon />} variant="tertiary" onClick={tildelSaksbehandlerWrapper}>
          Tildel meg
        </Button>
      )}
    </>
  )
}
