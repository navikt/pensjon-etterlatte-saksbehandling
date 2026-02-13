import { ArrowUndoIcon } from '@navikt/aksel-icons'
import { Alert, Box, Button, InlineMessage, VStack } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { opprettEtteroppgjoerForbehandlingOppgave } from '~shared/api/etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { useState } from 'react'
import { VelgEtteroppgjoersAar } from '~components/etteroppgjoer/components/VelgEtteroppgjoersAar'

type IProps = {
  sakId: number
}

export function TilbakestillOgOpprettNyForbehandling({ sakId }: IProps) {
  const [valgtEtteroppgjoer, setValgtEtteroppgjoer] = useState<string>('')

  const [opprettForbehandlingOppgaveStatus, opprettForbehandlingOppgaveFetch] = useApiCall(
    opprettEtteroppgjoerForbehandlingOppgave
  )

  return (
    <Box marginBlock="5 0">
      <VStack gap="4">
        <Box background="surface-subtle" padding="5">
          <InlineMessage status="warning">
            I tilfelle forbehandling, eller etteroppgjøret er ferdigstilt med feil informasjon, kan etteroppgjøret
            tilbakestilles og ny forbehandling opprettes.
          </InlineMessage>

          <Box marginBlock="5 0">
            <VelgEtteroppgjoersAar
              sakId={sakId.toString()}
              value={valgtEtteroppgjoer}
              onChange={setValgtEtteroppgjoer}
            />
          </Box>

          <Box marginBlock="5 0">
            <Button
              loading={isPending(opprettForbehandlingOppgaveStatus)}
              disabled={!valgtEtteroppgjoer}
              variant="secondary"
              icon={<ArrowUndoIcon />}
              onClick={() =>
                opprettForbehandlingOppgaveFetch({
                  sakId,
                  inntektsaar: valgtEtteroppgjoer,
                })
              }
            >
              Tilbakestill etteroppgjøret og opprett ny forbehandling
            </Button>
          </Box>
        </Box>

        {mapResult(opprettForbehandlingOppgaveStatus, {
          pending: <Spinner label="Oppretter ny forbehandling" />,
          error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
          success: () => <Alert variant="success">Ny forbehandling er opprettet og etteroppgjøret tilbakestilt.</Alert>,
        })}
      </VStack>
    </Box>
  )
}
