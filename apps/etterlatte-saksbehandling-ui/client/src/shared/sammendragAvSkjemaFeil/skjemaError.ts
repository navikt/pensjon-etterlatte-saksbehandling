import { FieldError, FieldErrors } from 'react-hook-form'

interface SkjemaError {
  name: string
  message: string
}

const fieldErrorsSomListe = (errors: FieldErrors): Array<FieldError> => {
  return Object.values(errors).flatMap((value?: unknown) => {
    if (!value) {
      return []
    }

    const kanskjeFieldError = value as FieldError

    if (kanskjeFieldError?.type && typeof kanskjeFieldError.type !== 'object') {
      return kanskjeFieldError
    } else {
      return fieldErrorsSomListe(value)
    }
  })
}

export const formaterFieldErrors = (errors: FieldErrors): Array<SkjemaError> => {
  return fieldErrorsSomListe(errors)
    .filter((error) => !!error)
    .map((error) => ({ name: error.ref!.name, message: error.message! }))
}
