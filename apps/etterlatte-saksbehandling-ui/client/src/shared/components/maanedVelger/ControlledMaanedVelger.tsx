import React, { ReactNode, useEffect, useState } from 'react'
import { MonthPicker, MonthValidationT, useMonthpicker } from '@navikt/ds-react'
import { Control, FieldValues, Path, useController } from 'react-hook-form'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { isEqual } from 'date-fns'

interface Props<T extends FieldValues> {
  name: Path<T>
  label: string
  control: Control<T>
  fromDate?: Date
  toDate?: Date
  validate?: (maaned: Date) => string | undefined
  required?: boolean
}

export const ControlledMaanedVelger = <T extends FieldValues>({
  name,
  label,
  control,
  fromDate,
  toDate,
  validate,
  required = false,
}: Props<T>): ReactNode => {
  const {
    field,
    fieldState: { error },
  } = useController({
    name,
    control,
    rules: { validate, required: { value: required, message: 'Må fylles ut' } },
  })

  const [, setDateError] = useState<MonthValidationT | null>(null)

  const { monthpickerProps, inputProps, setSelected, selectedMonth } = useMonthpicker({
    onMonthChange: (date: Date) => {
      date && field.onChange(date)
    },
    defaultSelected: field.value ? new Date(field.value) : null,
    fromDate: fromDate ?? undefined,
    toDate: toDate ?? undefined,
    locale: 'nb',
    onValidate: (val) => {
      if (val.isEmpty) field.onChange(null)
      else setDateError(val)
    },
  } as UseMonthPickerOptions)

  useEffect(() => {
    // Dette tillater å sette value for feltet via setValue utenfor komponenten
    if (selectedMonth && !field.value) {
      setSelected(undefined)
    } else if (selectedMonth && !isEqual(new Date(field.value), selectedMonth)) {
      setSelected(new Date(field.value))
    } else if (field.value && !selectedMonth && inputProps.value?.toString().length === 0) {
      setSelected(new Date(field.value))
    }
  }, [field, selectedMonth])

  return (
    <MonthPicker {...monthpickerProps}>
      <MonthPicker.Input {...inputProps} id={field.name} label={label} error={error?.message} />
    </MonthPicker>
  )
}
