import React, { ReactNode, useEffect, useState } from 'react'
import { DatePicker, DateValidationT, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { formatDateToLocaleDateOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'
import { isEqual } from 'date-fns'

export const ControlledDatoVelger = <T extends FieldValues>({
  name,
  label,
  description,
  control,
  errorVedTomInput,
  readOnly,
  shouldUnregister = false,
}: {
  name: Path<T>
  label: string
  description?: string
  control: Control<T>
  errorVedTomInput?: string
  readOnly?: boolean
  shouldUnregister?: boolean
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
        if (errorVedTomInput && !value) {
          return errorVedTomInput
        }
        return undefined
      },
    },
    shouldUnregister: shouldUnregister,
  })

  const { datepickerProps, inputProps, setSelected, selectedDay } = useDatepicker({
    onDateChange: (date: Date) => {
      date && field.onChange(formatDateToLocaleDateOrEmptyString(date))
    },
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    onValidate: setDateError,
    defaultSelected: field.value ? new Date(field.value) : undefined,
  } as UseDatepickerOptions)

  useEffect(() => {
    // Dette tillater å sette value for feltet via setValue utenfor komponenten
    if (selectedDay && !field.value) {
      setSelected(undefined)
    } else if (selectedDay && !isEqual(new Date(field.value), selectedDay)) {
      setSelected(new Date(field.value))
    } else if (field.value && !selectedDay && inputProps.value?.toString().length === 0) {
      setSelected(new Date(field.value))
    }
  }, [field, selectedDay])

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input
        {...inputProps}
        id={field.name}
        label={label}
        description={description}
        error={error?.message}
        readOnly={readOnly}
      />
    </DatePicker>
  )
}
