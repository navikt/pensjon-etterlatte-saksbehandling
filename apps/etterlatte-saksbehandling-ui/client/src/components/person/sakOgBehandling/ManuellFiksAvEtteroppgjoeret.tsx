import { ArrowUndoIcon } from '@navikt/aksel-icons'
import { Alert, Box, Button, InlineMessage, TextField, VStack } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { manuellFiksAvEtteroppgjoeret } from '~shared/api/etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { useState } from 'react'
import { VelgEtteroppgjoersAar } from '~components/etteroppgjoer/components/utils/VelgEtteroppgjoersAar'

type IProps = {
  sakId: number
}

export function ManuellFiksAvEtteroppgjoeret({ sakId }: IProps) {
  const [valgtEtteroppgjoer, setValgtEtteroppgjoer] = useState<string>('')
  const [behandlingerSomSkalHoppesOver, setBehandlingerSomSkalHoppesOver] = useState<string>('[]')

  const [manuellFiksAvEtteroppgjoeretStatus, manuellFiksAvEtteroppgjoeretRequest] =
    useApiCall(manuellFiksAvEtteroppgjoeret)

  return (
    <Box marginBlock="space-20 space-0">
      <VStack gap="space-16">
        <Box background="neutral-soft" padding="space-20">
          <InlineMessage status="warning">
            Obs! Dette skal kun brukes av svært svært få for å fikse i etteroppgjør som har blitt feil.
          </InlineMessage>

          <Box marginBlock="space-20 space-0">
            <VelgEtteroppgjoersAar
              sakId={sakId.toString()}
              value={valgtEtteroppgjoer}
              onChange={setValgtEtteroppgjoer}
            />
          </Box>

          <TextField
            label="Etteroppgjør å hoppe over"
            value={behandlingerSomSkalHoppesOver}
            onChange={(e) => {
              setBehandlingerSomSkalHoppesOver(e.currentTarget.value)
            }}
          />

          <Box marginBlock="space-20 space-0">
            <Button
              loading={isPending(manuellFiksAvEtteroppgjoeretStatus)}
              disabled={!valgtEtteroppgjoer}
              variant="secondary"
              icon={<ArrowUndoIcon />}
              onClick={() =>
                manuellFiksAvEtteroppgjoeretRequest({
                  sakId,
                  inntektsaar: valgtEtteroppgjoer,
                  behandlingerSomSkalHoppesOver: behandlingerSomSkalHoppesOver,
                })
              }
            >
              Opprett etteroppgjør revurdering med manuell fiks
            </Button>
          </Box>
        </Box>

        {mapResult(manuellFiksAvEtteroppgjoeretStatus, {
          pending: <Spinner label="Oppretter ny revurdering" />,
          error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
          success: () => <Alert variant="success">Ny etteroppgjør revurdering er opprettet.</Alert>,
        })}
      </VStack>
    </Box>
  )
}
