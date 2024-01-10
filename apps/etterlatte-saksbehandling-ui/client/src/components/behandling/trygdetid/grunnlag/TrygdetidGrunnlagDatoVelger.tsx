import React, { ReactNode, useState } from 'react'
import { DatePicker, DateValidationT, useDatepicker } from '@navikt/ds-react'
import { UseDatepickerOptions } from '@navikt/ds-react/esm/date/hooks/useDatepicker'
import { Control, useController } from 'react-hook-form'
import { format } from 'date-fns'
import { OppdaterTrygdetidGrunnlag } from '~shared/api/trygdetid'

export const TrygdetidGrunnlagDatoVelger = ({
  name,
  label,
  control,
}: {
  name: 'periodeFra' | 'periodeTil'
  label: string
  control: Control<OppdaterTrygdetidGrunnlag>
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
          return 'Obligatorisk'
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

const formatDateToLocaleDateOrEmptyString = (date: Date | undefined) =>
  date === undefined ? '' : format(date, 'yyyy-MM-dd')
