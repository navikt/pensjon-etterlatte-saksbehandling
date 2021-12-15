import React, { useState } from 'react'
import { Heading } from '@navikt/ds-react'
import { IOppgaveFelt, IOppgaveFelter } from './oppgavefelter'
import { useAsyncDebounce } from 'react-table'
import { Input, Select } from 'nav-frontend-skjema'
import { IPar } from '../../typer/oppgavebenken'
import '../../App.css'
import styled from 'styled-components'

type Props = {
  oppgaveFelter: IOppgaveFelter
  settOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
  settGlobalFilter: (value: string) => void
}

const FilterElement = styled.div`
  margin-bottom: 1rem;
  justify-items: flex-start;
  width: 200px;
`

const FilterWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 1rem;
`

const OppgaveHeader: React.FC<Props> = ({ oppgaveFelter, settOppgaveFelter, settGlobalFilter }) => {
  return (
    <>
      <Heading size={'medium'} spacing>
        Oppgavebenken
      </Heading>
      <GlobalFilter settGlobalFilter={settGlobalFilter} />
      <br />
      <ColumnFilters
        oppgaveFelter={oppgaveFelter}
        settOppgaveFelter={settOppgaveFelter}
        settGlobalFilter={settGlobalFilter}
      />

      <br />
    </>
  )
}

const ColumnFilters: React.FC<Props> = ({ oppgaveFelter, settOppgaveFelter, settGlobalFilter }) => {
  return (
    <FilterWrapper>
      {Object.values(oppgaveFelter)
        .filter((oppgaveFelt: IOppgaveFelt) => oppgaveFelt.filter)
        .map((oppgaveFelt: IOppgaveFelt) => {
          switch (oppgaveFelt.filter?.type) {
            case 'string':
              return <div />
            case 'select':
              return (
                <SelectColumnFilter
                  oppgaveFelt={oppgaveFelt}
                  liste={oppgaveFelt.filter?.nedtrekksliste}
                  oppgaveFelter={oppgaveFelter}
                  settOppgaveFelter={settOppgaveFelter}
                />
              )
          }
        })}
    </FilterWrapper>
  )
}

const GlobalFilter = ({ settGlobalFilter }: { settGlobalFilter: (value: string) => void }) => {
  const [value, setValue] = useState('')
  const onChange = useAsyncDebounce((value) => {
    settGlobalFilter(value || undefined)
  }, 200)

  return (
    <FilterElement>
      <Input
        bredde={'L'}
        label={'Søk: '}
        value={value}
        onChange={(e) => {
          setValue(e.target.value)
          onChange(e.target.value)
        }}
        placeholder={'Søk i hele tabellen'}
      />
    </FilterElement>
  )
}

export function SelectColumnFilter({
  oppgaveFelt,
  liste,
  oppgaveFelter,
  settOppgaveFelter,
}: {
  oppgaveFelt: IOppgaveFelt
  liste: Record<any, IPar> | undefined
  oppgaveFelter: IOppgaveFelter
  settOppgaveFelter: (oppgaver: IOppgaveFelter) => void
}) {
  const options = Object.values(liste ? liste : '')

  return (
    <FilterElement>
      <Select
        label={oppgaveFelt.label}
        value={oppgaveFelt.filter?.selectedValue}
        key={oppgaveFelt.noekkel}
        onChange={(e) => {
          settFilterVerdi(oppgaveFelt, e.target.value, oppgaveFelter, settOppgaveFelter)
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

const settFilterVerdi = (
  oppgaveFelt: IOppgaveFelt,
  nyVerdi: string,
  oppgaveFelter: IOppgaveFelter,
  settOppgaveFelter: (oppgaver: IOppgaveFelter) => void
) => {
  if (oppgaveFelt.filter) {
    const oppdaterteOppgaveFelter = {
      ...oppgaveFelter,
      [oppgaveFelt.noekkel]: {
        ...oppgaveFelt,
        filter: {
          ...oppgaveFelt.filter,
          selectedValue: nyVerdi,
        },
      },
    }

    settOppgaveFelter(oppdaterteOppgaveFelter)
  }
}

export default OppgaveHeader
