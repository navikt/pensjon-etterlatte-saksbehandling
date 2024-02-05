import React from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'
import { OPPGAVESTATUSFILTER } from '~components/oppgavebenk/filter/oppgavelistafiltre'

interface Props {
  value: Array<string>
  onChange: (statuser: Array<string>) => void
}

export const VelgOppgavestatuser = ({ value, onChange }: Props) => {
  const onOppgavestatusSelected = (option: string, isSelected: boolean) => {
    if (isSelected) {
      onChange([...value, option])
    } else {
      onChange([...value.filter((val) => val !== option)])
    }
  }

  return (
    <UNSAFE_Combobox
      label="Oppgavestatus"
      options={Object.entries(OPPGAVESTATUSFILTER).map(([, beskrivelse]) => beskrivelse)}
      selectedOptions={value}
      onToggleSelected={(option, isSelected) => onOppgavestatusSelected(option, isSelected)}
      isMultiSelect
    />
  )
}
