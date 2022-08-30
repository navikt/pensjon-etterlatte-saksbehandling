import { ErrorMessage, Label } from '@navikt/ds-react'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import React, { useRef } from 'react'
import styled from 'styled-components'

type Props = {
  label: string
  dato: Date | null
  setDato: (dato: Date | null) => void
  error: string | undefined
  setErrorUndefined: () => void
}

export const DatovelgerPeriode: React.FC<Props> = ({ label, dato, setDato, error, setErrorUndefined }) => {
  const ref: any = useRef(null)
  const toggleDatepicker = () => {
    ref.current.setOpen(true)
    ref.current.setFocus()
  }

  return (
    <Wrapper>
      <section>
        <Label style={{ marginBottom: '8px' }} size={'small'}>
          {label}
        </Label>

        <Datovelger>
          <DatePicker
            ref={ref}
            dateFormat={'dd.MM.yyyy'}
            placeholderText={'dd.mm.åååå'}
            selected={dato}
            onChange={(date) => {
              setDato(date)
              date !== undefined && setErrorUndefined()
            }}
            autoComplete="off"
            preventOpenOnFocus={true}
            className={'skjemaelement__input'}
          />

          <KalenderIkon
            tabIndex={0}
            onClick={toggleDatepicker}
            role="button"
            title="Åpne datovelger"
            aria-label="Åpne datovelger"
          >
            <Calender color="white" />
          </KalenderIkon>
        </Datovelger>
        {error && <ErrorMessage size={'small'}>{error}</ErrorMessage>}
      </section>
    </Wrapper>
  )
}

export const Wrapper = styled.div`
  justify-items: flex-start;
  width: 200px;
  margin-right: 1rem;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;
  margin-bottom: 0.5rem;

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
