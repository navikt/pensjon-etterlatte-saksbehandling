import { FilterElement, FilterWrapper } from '../styled'
import { Input } from 'nav-frontend-skjema'
import React from 'react'
import { IOppgaveFelt, IOppgaveFelter } from '../typer/oppgavefelter'
import { settFilterVerdi } from './setFilterVerdi'

type Props = {
  oppgaveFelt: IOppgaveFelt
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
}

const StringColumnFilter: React.FC<Props> = ({ oppgaveFelt, oppgaveFelter, setOppgaveFelter }) => {
  return (
    <FilterWrapper>
      <FilterElement>
        <Input
          bredde={'L'}
          label={oppgaveFelt.label}
          value={oppgaveFelt.filter?.selectedValue}
          onChange={(e) => {
            settFilterVerdi(oppgaveFelt, e.target.value, oppgaveFelter, setOppgaveFelter)
          }}
          placeholder={'Søk '}
        />
      </FilterElement>
    </FilterWrapper>
  )
}

export default StringColumnFilter
