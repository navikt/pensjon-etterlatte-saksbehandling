import React, { ReactNode, useState } from 'react'
import { MonthPicker, MonthValidationT, useMonthpicker } from '@navikt/ds-react'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'

interface Props<T extends FieldValues> {
  name: Path<T>
  label: string
  control: Control<T>
  fromDate?: Date
  toDate?: Date
  validate: (maaned: Date) => string | undefined
}

export const ControlledMaanedVelger = <T extends FieldValues>({
  name,
  label,
  control,
  fromDate,
  toDate,
  validate,
}: Props<T>): ReactNode => {
  const {
    field,
    fieldState: { error },
  } = useController({
    name,
    control,
    rules: { validate },
  })

  const [, setDateError] = useState<MonthValidationT | null>(null)

  const { monthpickerProps, inputProps } = useMonthpicker({
    onMonthChange: (date: Date) => {
      date && field.onChange(date)
    },
    defaultSelected: field.value ? new Date(field.value) : null,
    fromDate: fromDate ?? undefined,
    toDate: toDate ?? undefined,
    locale: 'nb',
    onValidate: setDateError,
  } as UseMonthPickerOptions)

  return (
    <MonthPicker {...monthpickerProps}>
      <MonthPicker.Input {...inputProps} id={field.name} label={label} error={error?.message} />
    </MonthPicker>
  )
}
