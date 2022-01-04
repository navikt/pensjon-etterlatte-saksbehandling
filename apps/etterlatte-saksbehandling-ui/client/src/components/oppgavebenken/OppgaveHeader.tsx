import React, { useEffect, useState } from 'react'
import { Button, Heading } from '@navikt/ds-react'
import { initialOppgaveFelter, IOppgaveFelt, IOppgaveFelter } from './oppgavefelter'
import { useAsyncDebounce } from 'react-table'
import { Input, Select } from 'nav-frontend-skjema'
import { IPar } from '../../typer/oppgavebenken'
import '../../App.css'
import styled from 'styled-components'

type Props = {
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
  setGlobalFilter: (value: string | undefined) => void
}

const FilterElement = styled.div`
  margin-bottom: 2rem;
  justify-items: flex-start;
  justify-items: flex-start;
  width: 200px;
  margin-right: 1rem;
`

const FilterWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`

const ButtonWrapper = styled.div`
  margin-bottom: 3rem;
`

const OppgaveHeader: React.FC<Props> = ({ oppgaveFelter, setOppgaveFelter, setGlobalFilter }) => {
  const [resetGlobalInput, setResetGlobalInput] = useState<boolean>(false)

  return (
    <>
      <Heading size={'medium'} spacing>
        Oppgavebenken
      </Heading>
      <FilterWrapper>
        <GlobalFilter
          setGlobalFilter={setGlobalFilter}
          resetGlobalInput={resetGlobalInput}
          setResetGlobalInput={setResetGlobalInput}
        />
      </FilterWrapper>
      <ColumnFilters oppgaveFelter={oppgaveFelter} setOppgaveFelter={setOppgaveFelter} />
      <ButtonWrapper>
        <Button
          onClick={() => {
            setOppgaveFelter(initialOppgaveFelter())
            setResetGlobalInput(true)
          }}
          variant={'secondary'}
        >
          Tilbakestill alle filtre
        </Button>
      </ButtonWrapper>
    </>
  )
}

const ColumnFilters: React.FC<Omit<Props, 'setGlobalFilter'>> = ({ oppgaveFelter, setOppgaveFelter }) => {
  return (
    <FilterWrapper>
      {Object.values(oppgaveFelter)
        .filter((oppgaveFelt: IOppgaveFelt) => oppgaveFelt.filter)
        .map((oppgaveFelt: IOppgaveFelt) => {
          switch (oppgaveFelt.filter?.type) {
            case 'string':
              return <div key={oppgaveFelt.noekkel} />
            case 'select':
              return (
                <SelectColumnFilter
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

const GlobalFilter = ({
  setGlobalFilter,
  resetGlobalInput,
  setResetGlobalInput,
}: {
  setGlobalFilter: (value: string | undefined) => void
  setResetGlobalInput: (valuse: boolean) => void
  resetGlobalInput: boolean
}) => {
  const [value, setValue] = useState('')
  const onChange = useAsyncDebounce((value) => {
    setGlobalFilter(value || undefined)
  }, 200)

  useEffect(() => {
    if (resetGlobalInput) {
      setValue('')
      setGlobalFilter(undefined)
    }
    return setResetGlobalInput(false)
  }, [resetGlobalInput])

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
  setOppgaveFelter,
}: {
  oppgaveFelt: IOppgaveFelt
  liste: Record<any, IPar> | undefined
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaver: IOppgaveFelter) => void
}) {
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

const settFilterVerdi = (
  oppgaveFelt: IOppgaveFelt,
  nyVerdi: string,
  oppgaveFelter: IOppgaveFelter,
  setOppgaveFelter: (oppgaver: IOppgaveFelter) => void
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

    setOppgaveFelter(oppdaterteOppgaveFelter)
  }
}

export default OppgaveHeader
