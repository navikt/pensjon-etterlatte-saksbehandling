import { IOppgaveFelt, IOppgaveFelter } from '../typer/oppgavefelter'
import { IPar } from '../typer/oppgavebenken'
import React from 'react'
import { FilterElement } from '../styled'
import { settFilterVerdi } from './setFilterVerdi'
import { Select } from '@navikt/ds-react'

type Props = {
  oppgaveFelt: IOppgaveFelt
  liste: Record<any, IPar> | undefined
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
}

const DropdownColumnFilter: React.FC<Props> = ({ oppgaveFelt, liste, oppgaveFelter, setOppgaveFelter }) => {
  const options = Object.values(liste ? liste : '')

  return (
    <FilterElement>
      <Select
        label={oppgaveFelt.label}
        value={oppgaveFelt.filter?.selectedValue}
        key={oppgaveFelt.noekkel}
        onChange={(e) => {
          settFilterVerdi(oppgaveFelt, e.target.value, oppgaveFelter, setOppgaveFelter)
        }}
        autoComplete="off"
      >
        {options.map((par: IPar) => {
          return (
            <option key={par.id} value={par.id}>
              {par.navn}
            </option>
          )
        })}
      </Select>
    </FilterElement>
  )
}

export default DropdownColumnFilter
