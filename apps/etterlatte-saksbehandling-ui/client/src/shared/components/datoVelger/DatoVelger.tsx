import { DatePicker, useDatepicker } from '@navikt/ds-react'
import React, { ReactNode } from 'react'

export const DatoVelger = ({
  value,
  onChange,
  label,
  description,
  error,
  disabled = false,
  readOnly = false,
  fromDate = undefined,
  toDate = undefined,
}: {
  value: Date | undefined
  onChange: (date: Date | undefined) => void
  label: ReactNode
  description?: ReactNode
  error?: ReactNode
  disabled?: boolean
  readOnly?: boolean
  fromDate?: Date | undefined
  toDate?: Date | undefined
}) => {
  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: (date: Date | undefined) => onChange(date),
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    defaultSelected: value,
    fromDate: fromDate,
    toDate: toDate,
  })

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input
        {...inputProps}
        label={label}
        description={description}
        error={error}
        disabled={disabled}
        readOnly={readOnly}
      />
    </DatePicker>
  )
}
