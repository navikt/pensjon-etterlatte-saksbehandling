import { DatePicker, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import React, { ReactNode } from 'react'

export const DatoVelger = ({
  value,
  onChange,
  label,
  description,
  error,
}: {
  value: Date | null
  onChange: (date: Date | null) => void
  label: string
  description?: ReactNode
  error?: ReactNode
}) => {
  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: (date: Date) => onChange(date),
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    defaultSelected: value,
  } as UseDatepickerOptions)

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input {...inputProps} label={label} description={description} error={error} />
    </DatePicker>
  )
}
