import { XMarkOctagonIcon } from '@navikt/aksel-icons'
import { Box, Button, InlineMessage } from '@navikt/ds-react'
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

      <Box background="surface-subtle" padding="5">
        <InlineMessage status="error">
          I tilfelle forbehandling er ferdigstilt med feil informasjon, kan etteroppgjøret tilbakestilles og ny
          forbehandling opprettes. Kun hvis vi venter på svar fra bruker.
        </InlineMessage>
        <Box marginBlock="5 0">
          <Button
            loading={isPending(opprettForbehandlingOppgaveStatus)}
            variant="secondary"
            onClick={() => opprettForbehandlingOppgaveFetch(sakId, () => hentNyeOppgaver())}
            icon={<XMarkOctagonIcon />}
          >
            Tilbakestill etteroppgjøret og opprett ny forbehandling
          </Button>
        </Box>
      </Box>
    </Box>
  )
}
