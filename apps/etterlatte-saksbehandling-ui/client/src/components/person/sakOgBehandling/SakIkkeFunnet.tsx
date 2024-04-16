import React from 'react'
import { ApiError } from '~shared/api/apiClient'
import { Container } from '~shared/styled'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { OpprettSakModal } from '~components/person/sakOgBehandling/OpprettSakModal'

export const SakIkkeFunnet = ({ error, fnr }: { error: ApiError; fnr: string }) => {
  return (
    <Container>
      {error.status === 404 ? (
        <>
          <ApiWarningAlert>{error.detail}</ApiWarningAlert>
          <OpprettSakModal fnr={fnr} />
        </>
      ) : (
        <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
      )}
    </Container>
  )
}
