import { Box, Button } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { opprettEtteroppgjoerForbehandlingOppgave } from '~shared/api/etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'

type IProps = {
  sakId: number
  hentNyeOppgaver: () => void
}

export function OpprettNyForbehandling({ sakId, hentNyeOppgaver }: IProps) {
  const [opprettForbehandlingOppgaveStatus, opprettForbehandlingOppgaveFetch] = useApiCall(
    opprettEtteroppgjoerForbehandlingOppgave
  )
  return (
    <Box>
      {mapResult(opprettForbehandlingOppgaveStatus, {
        pending: <Spinner label="Oppretter forbehandling" />,
        error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
      })}

      <Button
        loading={isPending(opprettForbehandlingOppgaveStatus)}
        variant="secondary"
        onClick={() => opprettForbehandlingOppgaveFetch(sakId, () => hentNyeOppgaver())}
      >
        Opprett ny etteroppgjør forbehandling
      </Button>
    </Box>
  )
}
