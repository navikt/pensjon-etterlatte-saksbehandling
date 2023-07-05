import { IOppgaveFelt, IOppgaveFelter } from '../typer/oppgavefelter'
import React, { useRef } from 'react'
import DatePicker from 'react-datepicker'
import { parse } from 'date-fns'
import styled from 'styled-components'
import { FilterElement } from '../styled'
import { settFilterVerdi } from './setFilterVerdi'
import { CalendarIcon } from '@navikt/aksel-icons'
import { Label } from '@navikt/ds-react'

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
      <DatoSection>
        <Label htmlFor={oppgaveFelt.noekkel}>{oppgaveFelt.label}</Label>
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
            className={'skjemaelement__input'}
            name={oppgaveFelt.noekkel}
          />
          <KalenderIkon
            tabIndex={0}
            onKeyPress={toggleDatepicker}
            onClick={toggleDatepicker}
            role="button"
            title="Åpne datovelger"
            aria-label="Åpne datovelger"
          >
            <CalendarIcon color="white" />
          </KalenderIkon>
        </Datovelger>
      </DatoSection>
    </FilterElement>
  )
}

const parseDate = (dato?: Date | string) => {
  if (!dato) return
  if (typeof dato === 'string') {
    return parse(dato, 'dd.MM.yyyy', new Date())
  } else {
    return dato
  }
}

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;

  input {
    border-right: none;
    border-radius: 4px 0 0 4px;
    width: 160px;
    height: 48px;
    text-indent: 4px;
  }
`

const KalenderIkon = styled.div`
  padding: 4px 10px;
  cursor: pointer;
  background-color: #0167c5;
  border: 1px solid #000;
  border-radius: 0 4px 4px 0;
  height: 48px;
  line-height: 42px;
`

const DatoSection = styled.section`
  display: grid;
  gap: 0.5em;
`

export default DateColumnFilter
