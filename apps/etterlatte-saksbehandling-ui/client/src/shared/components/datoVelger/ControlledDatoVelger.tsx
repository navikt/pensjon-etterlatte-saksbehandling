import React, { ReactNode, useState } from 'react'
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
        if (validationError) {
          return validationError
        }
      },
    },
    shouldUnregister: shouldUnregister,
  })

  const [validationError, setValidationError] = useState<string | null>(null)

  const onValidate = (validation: DateValidationT) => {
    setValidationError(validation.isInvalid ? 'Ugyldig dato' : null)
  }

  const { datepickerProps, inputProps, setSelected, selectedDay } = useDatepicker({
    onDateChange: (date: Date) => {
      field.onChange(date ? formatDateToLocaleDateOrEmptyString(date) : null)
    },
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    onValidate: onValidate,
    defaultSelected: field.value ? new Date(field.value) : undefined,
  } as UseDatepickerOptions)

  const handleBlur = () => {
    if (selectedDay && !field.value) {
      setSelected(undefined)
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
        label={label}
        description={description}
        error={error?.message}
        readOnly={readOnly}
        onBlur={handleBlur}
      />
    </DatePicker>
  )
}
