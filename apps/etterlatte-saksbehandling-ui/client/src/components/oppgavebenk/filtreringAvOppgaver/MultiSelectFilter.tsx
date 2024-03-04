import React from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'

interface Props {
  label: string
  options: Array<string>
  value: Array<string>
  onChange: (options: Array<string>) => void
}

export const MultiSelectFilter = ({ label, options, value, onChange }: Props) => {
  const onToggleSelected = (option: string, isSelected: boolean) => {
    if (isSelected) {
      onChange([...value, option])
    } else {
      onChange([...value.filter((val) => val !== option)])
    }
  }

  return (
    <UNSAFE_Combobox
      label={label}
      options={options}
      selectedOptions={value}
      onToggleSelected={onToggleSelected}
      isMultiSelect
    />
  )
}
