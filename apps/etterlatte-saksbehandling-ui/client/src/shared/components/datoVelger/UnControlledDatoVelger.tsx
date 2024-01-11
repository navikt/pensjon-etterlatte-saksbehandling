import { DatePicker, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import React, { ReactNode } from 'react'
import { format } from 'date-fns'

export const UnControlledDatoVelger = ({
  value,
  onChange,
  label,
  description,
  error,
  disabled = false,
  fromDate = undefined,
  toDate = undefined,
}: {
  value: Date | undefined
  onChange: (date: Date | undefined) => void
  label: ReactNode
  description?: ReactNode
  error?: ReactNode
  disabled?: boolean
  fromDate?: Date | undefined
  toDate?: Date | undefined
}) => {
  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: (date: Date) => onChange(date),
    locale: 'nb',
    inputFormat: 'dd.MM.yyyy',
    defaultSelected: value,
    fromDate: fromDate,
    toDate: toDate,
  } as UseDatepickerOptions)

  return (
    <DatePicker {...datepickerProps}>
      <DatePicker.Input {...inputProps} label={label} description={description} error={error} disabled={disabled} />
    </DatePicker>
  )
}

export const formatDateToLocaleDateOrEmptyString = (date: Date | undefined) =>
  date === undefined ? '' : format(date, 'yyyy-MM-dd')
