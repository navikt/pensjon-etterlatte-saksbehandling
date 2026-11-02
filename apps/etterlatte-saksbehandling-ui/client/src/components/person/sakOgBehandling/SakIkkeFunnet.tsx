import React from 'react'
import { ApiError } from '~shared/api/apiClient'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { OpprettSakModal } from '~components/person/sakOgBehandling/OpprettSakModal'
import { Box, VStack } from '@navikt/ds-react'

export const SakIkkeFunnet = ({
  error,
  fnr,
  falskIdentitetFeil,
}: {
  error: ApiError
  fnr: string
  falskIdentitetFeil?: ApiError
}) => {
  //TODO håndtere 401
  return (
    <Box padding="space-32">
      <VStack gap="space-16">
        {falskIdentitetFeil && <ApiErrorAlert>{falskIdentitetFeil.detail}</ApiErrorAlert>}
        {error.status === 404 ? (
          <>
            <ApiWarningAlert>{error.detail}</ApiWarningAlert>
            <OpprettSakModal fnr={fnr} />
          </>
        ) : (
          <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
        )}
      </VStack>
    </Box>
  )
}
