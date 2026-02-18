import React from 'react'
import { VStack, Button, Box, Heading } from '@navikt/ds-react'
import { SandboxIcon } from '@navikt/aksel-icons'

import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { finnOgOpprettEtteroppgjoer } from '~shared/api/etteroppgjoer'

type Props = {
  sakId: number
}

export const OpprettEtteroppgjoerIDev = ({ sakId }: Props) => {
  const [status, opprettFetch] = useApiCall(finnOgOpprettEtteroppgjoer)

  return (
    <Box background="surface-subtle" padding="5">
      <VStack gap="4">
        <Heading size="small">Finn og opprett etteroppgjør (kun i dev)</Heading>
        {status.status === 'error' && <ApiErrorAlert>{status.error.detail}</ApiErrorAlert>}

        <Box>
          <Button
            variant="secondary"
            icon={<SandboxIcon />}
            loading={isPending(status)}
            onClick={() => opprettFetch({ sakId })}
          >
            Opprett etteroppgjør (kun i dev)
          </Button>
        </Box>
      </VStack>
    </Box>
  )
}
