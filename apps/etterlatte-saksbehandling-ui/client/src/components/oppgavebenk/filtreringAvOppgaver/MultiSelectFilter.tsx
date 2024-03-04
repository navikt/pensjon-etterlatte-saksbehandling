import React from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'

interface Props {
  label: string
  options: Array<string>
  values: Array<string>
  onChange: (options: Array<string>) => void
}

export const MultiSelectFilter = ({ label, options, values, onChange }: Props) => {
  const onToggleSelected = (option: string, isSelected: boolean) => {
    if (isSelected) {
      onChange([...values, option])
    } else {
      onChange([...values.filter((val) => val !== option)])
    }
  }

  return (
    <UNSAFE_Combobox
      label={label}
      options={options}
      selectedOptions={typeof values === 'string' ? [values] : values}
      onToggleSelected={onToggleSelected}
      isMultiSelect
    />
  )
}
