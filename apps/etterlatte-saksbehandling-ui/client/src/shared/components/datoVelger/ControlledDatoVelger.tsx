import React, { ReactNode, useState } from 'react'
import { DatePicker, DateValidationT, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { formatDateToLocaleDateOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'

export const ControlledDatoVelger = <T extends FieldValues>({
  name,
  label,
  control,
  errorVedTomInput,
}: {
  name: Path<T>
  label: string
  control: Control<T>
  errorVedTomInput: string
}): ReactNode => {
  const [, setDateError] = useState<DateValidationT | null>(null)

  const {
    field,
    fieldState: { error },
  } = useController({
    name,
    control,
    rules: {
      validate: (value) => {
        if (!value) {
          return errorVedTomInput
        }
        return undefined
      },
    },
    defaultValue: undefined,
  })

  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: (date: Date) => {
      date && field.onChange(formatDateToLocaleDateOrEmptyString(date))
    },
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    onValidate: setDateError,
  } as UseDatepickerOptions)

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input {...inputProps} id={field.name} label={label} error={error?.message} />
    </DatePicker>
  )
}
