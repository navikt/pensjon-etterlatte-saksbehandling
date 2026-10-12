import React from 'react'
import { Alert } from '@navikt/ds-react'

export const FalskIdentitetAlert = ({ erFalskIdentitet }: { erFalskIdentitet?: boolean }) =>
  erFalskIdentitet && (
    <Alert variant="error" size="small">
      Identen er registrert som falsk identitet i Folkeregisteret
    </Alert>
  )
