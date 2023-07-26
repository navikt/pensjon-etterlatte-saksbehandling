import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { fjernSaksbehandlerApi } from '~shared/api/oppgaverny'
import { Button, Loader } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrashIcon } from '@navikt/aksel-icons'

export const RedigerSaksbehandler = (props: { saksbehandler: string; oppgaveId: string; sakId: number }) => {
  const { saksbehandler, oppgaveId, sakId } = props
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [fjernSaksbehandlerSvar, fjernSaksbehandler] = useApiCall(fjernSaksbehandlerApi)

  return (
    <>
      {saksbehandler}
      {user.ident === saksbehandler && (
        <>
          {isPending(fjernSaksbehandlerSvar) && <Loader size="small" title="Fjerner saksbehandler" />}
          {isSuccess(fjernSaksbehandlerSvar) && 'Saksbehandler er fjernet'}
          {isFailure(fjernSaksbehandlerSvar) && <ApiErrorAlert>Kunne ikke fjerne deg fra oppgaven</ApiErrorAlert>}
          {isInitial(fjernSaksbehandlerSvar) && (
            <Button
              icon={<TrashIcon />}
              variant="tertiary"
              onClick={() => fjernSaksbehandler({ oppgaveId: oppgaveId, sakId: sakId })}
            >
              Fjern meg som saksbehandler
            </Button>
          )}
        </>
      )}
    </>
  )
}
