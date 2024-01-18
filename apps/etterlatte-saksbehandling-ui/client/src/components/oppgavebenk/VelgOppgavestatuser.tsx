import React, { Dispatch, SetStateAction } from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'
import { OPPGAVESTATUSFILTER } from '~components/oppgavebenk/Oppgavelistafiltre'

interface Props {
  oppgavestatuserValgt: Array<string>
  setOppgavestatuserValgt: Dispatch<SetStateAction<Array<string>>>
}

export const VelgOppgavestatuser = ({ oppgavestatuserValgt, setOppgavestatuserValgt }: Props) => {
  const onOppgavestatusSelected = (option: string, isSelected: boolean) => {
    let nyOppgavestatusSelected: Array<string>

    if (isSelected) {
      nyOppgavestatusSelected = [...oppgavestatuserValgt, option]
    } else {
      nyOppgavestatusSelected = [...oppgavestatuserValgt.filter((val) => val !== option)]
    }

    setOppgavestatuserValgt(nyOppgavestatusSelected)
  }

  return (
    <UNSAFE_Combobox
      label="Oppgavestatus"
      options={OPPGAVESTATUSFILTER}
      selectedOptions={oppgavestatuserValgt}
      onToggleSelected={(option, isSelected) => onOppgavestatusSelected(option, isSelected)}
      isMultiSelect
    />
  )
}
