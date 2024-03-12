import { mapResult, Result } from '~shared/api/apiUtils'
import { Alert } from '@navikt/ds-react'
import React from 'react'

/**
 * Enkel Alert som viser en liten success- eller error-alert avhengig av resultatet.
 **/
export const ResultAlert = ({ result, success }: { result: Result<unknown>; success: string }) =>
  mapResult(result, {
    success: () => (
      <Alert variant="success" size="small">
        {success}
      </Alert>
    ),
    error: (error) => (
      <Alert variant="error" size="small">
        {error.detail || 'Ukjent feil oppsto'}
      </Alert>
    ),
  })
