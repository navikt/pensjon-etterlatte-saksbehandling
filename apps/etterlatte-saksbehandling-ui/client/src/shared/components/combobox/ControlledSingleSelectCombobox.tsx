import React, { ReactNode } from 'react'
import { Control, Controller, FieldValues, Path } from 'react-hook-form'
import { UNSAFE_Combobox } from '@navikt/ds-react'

interface Props<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  label: string
  options: Array<string>
  errorVedTomInput?: string
}

export const ControlledSingleSelectCombobox = <T extends FieldValues>({
  name,
  control,
  label,
  options,
  errorVedTomInput,
}: Props<T>): ReactNode => {
  return (
    <Controller
      name={name}
      control={control}
      rules={{
        required: errorVedTomInput
          ? {
              value: true,
              message: errorVedTomInput,
            }
          : undefined,
      }}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <UNSAFE_Combobox
          label={label}
          options={options}
          selectedOptions={[value]}
          onToggleSelected={(option, isSelected) => {
            isSelected ? onChange(option) : onChange('')
          }}
          error={error?.message}
          shouldAutocomplete
        />
      )}
    />
  )
}
