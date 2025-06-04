import { ErrorSummary } from '@navikt/ds-react'
import { FieldErrors, FieldValues } from 'react-hook-form'
import { formaterFieldErrors } from '~shared/sammendragAvSkjemaFeil/skjemaError'

interface Props<T extends FieldValues> {
  errors: FieldErrors<T>
}

export const SammendragAvSkjemaFeil = <T extends FieldValues>({ errors }: Props<T>) => {
  return (
    !!Object.keys(errors)?.length && (
      <ErrorSummary>
        {formaterFieldErrors(errors).map((error) => (
          <ErrorSummary.Item key={error.name} href={`#${error.name}`}>
            {error.message}
          </ErrorSummary.Item>
        ))}
      </ErrorSummary>
    )
  )
}
