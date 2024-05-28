import React from 'react'
import { ApiError } from '~shared/api/apiClient'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { OpprettSakModal } from '~components/person/sakOgBehandling/OpprettSakModal'
import { Box } from '@navikt/ds-react'

export const SakIkkeFunnet = ({ error, fnr }: { error: ApiError; fnr: string }) => {
  return (
    <Box padding="8">
      {error.status === 404 ? (
        <>
          <ApiWarningAlert>{error.detail}</ApiWarningAlert>
          <OpprettSakModal fnr={fnr} />
        </>
      ) : (
        <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
      )}
    </Box>
  )
}
