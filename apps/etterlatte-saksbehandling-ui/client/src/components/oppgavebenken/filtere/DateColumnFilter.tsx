import { IOppgaveFelt, IOppgaveFelter } from '../typer/oppgavefelter'
import React, { useRef } from 'react'
import { Label } from 'nav-frontend-skjema'
import DatePicker from 'react-datepicker'
import styled from 'styled-components'
import { FilterElement } from '../styled'
import moment from 'moment'
import { settFilterVerdi } from './setFilterVerdi'

type Props = {
  oppgaveFelt: IOppgaveFelt
  oppgaveFelter: IOppgaveFelter
  setOppgaveFelter: (oppgaveFelter: IOppgaveFelter) => void
}

const DateColumnFilter: React.FC<Props> = ({ oppgaveFelt, oppgaveFelter, setOppgaveFelter }) => {
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

const parseDate = (dato?: Date | string) => {
  if (!dato) return
  else if (typeof dato === 'string') return moment(dato, moment.defaultFormat).toDate()
  else return dato
}

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

export default DateColumnFilter
