import React, { useEffect, useRef, useState } from 'react'
import { Button, Heading } from '@navikt/ds-react'
import { initialOppgaveFelter, IOppgaveFelt, IOppgaveFelter } from './oppgavefelter'
import { useAsyncDebounce } from 'react-table'
import { Input, Label, Select } from 'nav-frontend-skjema'
import { IPar } from '../../typer/oppgavebenken'
import '../../App.css'
import styled from 'styled-components'
import DatePicker from 'react-datepicker'
import 'react-datepicker/dist/react-datepicker.css'
import '../../index.css'
import moment from 'moment'
moment.defaultFormat = 'DD.MM.YYYY'

type Props = {
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
  setGlobalFilter: (value: string | undefined) => void
}

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

const parseDate = (dato?: Date | string) => {
  if (!dato) return
  else if (typeof dato === 'string') return moment(dato, moment.defaultFormat).toDate()
  else return dato
}

export function DateColumnFilter({
  oppgaveFelt,
  oppgaveFelter,
  setOppgaveFelter,
}: {
  oppgaveFelt: IOppgaveFelt
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaver: IOppgaveFelter) => void
}) {
  const datepickerRef: any = useRef(null)
  const toggleDatepicker = () => {
    datepickerRef.current.setOpen(true)
    datepickerRef.current.setFocus()
  }

  return (
    <FilterElement>
      <section>
        <Label htmlFor={oppgaveFelt.label}>{oppgaveFelt.label}</Label>

        <Datovelger>
          <DatePicker
            ref={datepickerRef}
            id={oppgaveFelt.noekkel}
            dateFormat={'dd.MM.yyyy'}
            placeholderText={'dd.mm.åååå'}
            selected={parseDate(oppgaveFelt.filter?.selectedValue)}
            onChange={(date) => settFilterVerdi(oppgaveFelt, date ? date : '', oppgaveFelter, setOppgaveFelter)}
            autoComplete="off"
            preventOpenOnFocus={true}
            className={'skjemaelement__input test'}
          />

          <KalenderIkon
            tabIndex={0}
            onKeyPress={toggleDatepicker}
            onClick={toggleDatepicker}
            role="button"
            title="Åpne datovelger"
            aria-label="Åpne datovelger"
          >
            <svg height="24px" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M6 7V5H2v5h20V5h-4v2a1 1 0 11-2 0V5H8v2a1 1 0 11-2 0zm10-4H8V1a1 1 0 10-2 0v2H2a2 2 0 00-2 2v17a2 2 0 002 2h20a2 2 0 002-2V5a2 2 0 00-2-2h-4V1a1 1 0 10-2 0v2zM2 12v10h20V12H2zm6 3a1 1 0 00-1-1H5a1 1 0 100 2h2a1 1 0 001-1zm-1 3a1 1 0 110 2H5a1 1 0 110-2h2zm6-4h-2a1 1 0 100 2h2a1 1 0 100-2zm-2 4h2a1 1 0 110 2h-2a1 1 0 110-2zm9-3a1 1 0 00-1-1h-2a1 1 0 100 2h2a1 1 0 001-1zm-4 4a1 1 0 011-1h2a1 1 0 110 2h-2a1 1 0 01-1-1z"
                fill="#fff"
              ></path>
            </svg>
          </KalenderIkon>
        </Datovelger>
      </section>
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
  nyVerdi: string | Date,
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

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;

  input {
    border-right: none;
    border-radius: 4px 0 0 4px;
    width: 154px;
  }
`
const KalenderIkon = styled.div`
  padding: 4px 10px;
  cursor: pointer;
  background-color: #0167c5;
  border: 1px solid #000;
  border-radius: 0 4px 4px 0;
  height: 40px;
  line-height: 42px;
`

export default OppgaveHeader
