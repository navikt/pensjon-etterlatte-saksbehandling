import { useAppSelector } from '~store/Store'
import { isFailure, isInitial, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Oppgavetype, tildelSaksbehandlerApi } from '~shared/api/oppgaverny'
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

  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [tildelSaksbehandlerSvar, tildelSaksbehandler] = useApiCall(tildelSaksbehandlerApi)

  const tildelSaksbehandlerWrapper = () => {
    const nysaksbehandler = { saksbehandler: user.ident, versjon }

    tildelSaksbehandler({ oppgaveId, type, nysaksbehandler }, () => {
      console.log('start timeout')
      setTimeout(() => {
        console.log('oppdater tildeling')
        oppdaterTildeling(oppgaveId, user.ident)
      }, 3000)
    })
  }

  return (
    <>
      {isPending(tildelSaksbehandlerSvar) && <Loader size="small" title="Setter saksbehandler" />}
      {isSuccess(tildelSaksbehandlerSvar) && (
        <Alert variant="success" size={'small'}>
          Oppgave tildelt deg
        </Alert>
      )}
      {isFailure(tildelSaksbehandlerSvar) && (
        <Alert variant={'error'} size={'small'}>
          Tildeling feilet
        </Alert>
      )}
      {isInitial(tildelSaksbehandlerSvar) && (
        <Button icon={<PersonIcon />} variant="tertiary" size={'small'} onClick={tildelSaksbehandlerWrapper}>
          Tildel meg
        </Button>
      )}
    </>
  )
}
