import React, { useState } from 'react'
import { VStack, Button, Box, Heading } from '@navikt/ds-react'
import { SandboxIcon } from '@navikt/aksel-icons'

import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { opprettEtteroppgjoerForbehandlingIDev } from '~shared/api/etteroppgjoer'
import { VelgEtteroppgjoersAar } from '~components/etteroppgjoer/components/VelgEtteroppgjoersAar'

type Props = {
  sakId: number
}

export const OpprettEtteroppgjoerForbehandlingIDev = ({ sakId }: Props) => {
  const [valgtEtteroppgjoer, setValgtEtteroppgjoer] = useState<string>('')

  const [status, opprettFetch] = useApiCall(opprettEtteroppgjoerForbehandlingIDev)

  return (
    <Box maxWidth="50rem" borderColor="border-subtle" borderWidth="1" padding="5">
      <VStack gap="4">
        <Heading size="small">Opprett etteroppgjør forbehandling (kun i dev)</Heading>
        <VelgEtteroppgjoersAar sakId={sakId.toString()} value={valgtEtteroppgjoer} onChange={setValgtEtteroppgjoer} />

        {status.status === 'error' && <ApiErrorAlert>{status.error.detail}</ApiErrorAlert>}

        <Button
          variant="secondary"
          icon={<SandboxIcon />}
          loading={isPending(status)}
          disabled={!valgtEtteroppgjoer}
          onClick={() => opprettFetch({ sakId, inntektsaar: valgtEtteroppgjoer })}
        >
          Opprett etteroppgjør forbehandling (kun i dev)
        </Button>
      </VStack>
    </Box>
  )
}
