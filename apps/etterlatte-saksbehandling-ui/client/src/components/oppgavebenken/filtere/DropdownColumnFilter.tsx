import { IOppgaveFelt, IOppgaveFelter } from '../oppgavefelter'
import { IPar } from '../../../typer/oppgavebenken'
import { Select } from 'nav-frontend-skjema'
import React from 'react'
import { FilterElement } from '../styled'
import { settFilterVerdi } from './setFilterVerdi'

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
