import { useAppSelector } from '~store/Store'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { tildelSaksbehandlerApi } from '~shared/api/oppgaverny'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Button } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'

export const TildelSaksbehandler = (props: { id: string }) => {
  const { id } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [tildelSaksbehandlerSvar, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)
  const tildelSaksbehandlerWrapper = () => {
    tildelSaksbehandler({ oppgaveId: id, saksbehandler: user.ident })
  }
  return (
    <>
      {isPending(tildelSaksbehandlerSvar) && <>Setter saksbehandler</>}
      {isSuccess(tildelSaksbehandlerSvar) && 'Saksbehandler er endret'}
      {isFailure(tildelSaksbehandlerSvar) && <ApiErrorAlert>Kunne ikke tildele deg denne oppgaven</ApiErrorAlert>}
      {!isSuccess(tildelSaksbehandlerSvar) && (
        <Button icon={<PersonIcon />} variant="tertiary" onClick={tildelSaksbehandlerWrapper}>
          Tildel meg
        </Button>
      )}
    </>
  )
}
