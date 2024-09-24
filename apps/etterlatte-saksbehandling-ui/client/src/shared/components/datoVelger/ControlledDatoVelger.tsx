import React, { ReactNode } from 'react'
import { DatePicker, useDatepicker } from '@navikt/ds-react'
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
  required = true,
  size = 'medium',
  toDate,
}: {
  name: Path<T>
  label: string
  description?: string
  control: Control<T>
  errorVedTomInput?: string
  readOnly?: boolean
  shouldUnregister?: boolean
  required?: boolean
  size?: 'small' | 'medium'
  toDate?: Date
}): ReactNode => {
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
      required: { value: required, message: errorVedTomInput || 'Må fylles ut' },
    },
    shouldUnregister: shouldUnregister,
  })

  const { datepickerProps, inputProps, setSelected, selectedDay } = useDatepicker({
    onDateChange: (date: Date) => {
      date && field.onChange(formatDateToLocaleDateOrEmptyString(date))
    },
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    defaultSelected: field.value ? new Date(field.value) : undefined,
    toDate: toDate ?? undefined,
  } as UseDatepickerOptions)

  const handleBlur = () => {
    if ((selectedDay && !field.value) || (!selectedDay && !required)) {
      setSelected(undefined)
      if (!required) field.onChange('')
    } else if (selectedDay && !isEqual(new Date(field.value), selectedDay)) {
      setSelected(new Date(field.value))
    } else if (field.value && !selectedDay && inputProps.value?.toString().length === 0) {
      setSelected(new Date(field.value))
    }
  }
  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input
        {...inputProps}
        id={field.name}
        label={required ? label : `${label} (valgfritt)`}
        description={description}
        error={error?.message}
        readOnly={readOnly}
        onBlur={handleBlur}
        size={size}
      />
    </DatePicker>
  )
}
