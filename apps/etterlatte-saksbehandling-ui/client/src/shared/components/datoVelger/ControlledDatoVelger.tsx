import React, { ReactNode, useState } from 'react'
import { DatePicker, DateValidationT, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { formatDateToLocaleDateOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'

export const ControlledDatoVelger = <T extends FieldValues>({
  name,
  label,
  control,
  fromDate,
  toDate,
  validate,
}: {
  name: Path<T>
  label: string | ReactNode
  control: Control<T>
  fromDate?: Date
  toDate?: Date
  validate: (dato: DateValidationT) => string | undefined
}): ReactNode => {
  const [, setDateError] = useState<DateValidationT | null>(null)

  const {
    field,
    fieldState: { error },
  } = useController({
    name,
    control,
    rules: {
      validate,
    },
  })

  const { datepickerProps, inputProps } = useDatepicker({
    defaultSelected: field.value ? new Date(field.value) : null,
    fromDate: fromDate ?? undefined,
    toDate: toDate ?? undefined,
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
