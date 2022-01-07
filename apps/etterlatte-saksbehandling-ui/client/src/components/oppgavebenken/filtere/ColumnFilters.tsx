import React from 'react'
import { IOppgaveFelt, IOppgaveFelter } from '../oppgavefelter'
import DateColumnFilter from './DateColumnFilter'
import DropdownColumnFilter from './DropdownColumnFilter'
import { FilterWrapper } from '../styled'

type Props = {
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
}

const ColumnFilters: React.FC<Props> = ({ oppgaveFelter, setOppgaveFelter }) => {
  return (
    <FilterWrapper>
      {Object.values(oppgaveFelter)
        .filter((oppgaveFelt: IOppgaveFelt) => oppgaveFelt.filter)
        .map((oppgaveFelt: IOppgaveFelt) => {
          switch (oppgaveFelt.filter?.type) {
            case 'dato':
              return (
                <DateColumnFilter
                  key={oppgaveFelt.noekkel}
                  oppgaveFelt={oppgaveFelt}
                  oppgaveFelter={oppgaveFelter}
                  setOppgaveFelter={setOppgaveFelter}
                />
              )
            case 'string':
              return <div key={oppgaveFelt.noekkel} />
            case 'select':
              return (
                <DropdownColumnFilter
                  key={oppgaveFelt.noekkel}
                  oppgaveFelt={oppgaveFelt}
                  liste={oppgaveFelt.filter?.nedtrekksliste}
                  oppgaveFelter={oppgaveFelter}
                  setOppgaveFelter={setOppgaveFelter}
                />
              )
            default:
              return <div key={oppgaveFelt.noekkel} />
          }
        })}
    </FilterWrapper>
  )
}

export default ColumnFilters
