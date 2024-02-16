import React, { ReactNode } from 'react'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { TextField } from '@navikt/ds-react'

export const ControlledTextField = <T extends FieldValues>({
  name,
  control,
  label,
  description,
  readOnly,
  validate,
}: {
  name: Path<T>
  control: Control<T>
  label: string | ReactNode
  description?: string | ReactNode
  validate: (input: string) => string | undefined
  readOnly?: boolean
}): ReactNode => {
  const {
    field,
    fieldState: { error },
  } = useController({
    name,
    control,
    rules: { validate },
  })

  return <TextField {...field} label={label} description={description} readOnly={readOnly} error={error?.message} />
}
