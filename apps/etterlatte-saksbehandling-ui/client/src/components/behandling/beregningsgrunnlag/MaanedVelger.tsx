import React, { useMemo, useState } from 'react'
import DatePicker from 'react-datepicker'
import { Calender } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Label } from '@navikt/ds-react'
import { setDefaultOptions } from 'date-fns'
import { nb } from 'date-fns/locale'

setDefaultOptions({ locale: nb })

const maanedVelgerDefaultProps = {
  format: 'MMMM yyyy',
  placeholder: 'Velg måned og år',
}

type MaanedVelgerProps = {
  onChange: (date: Date | null) => void
  value?: Date
  label: string
} & typeof maanedVelgerDefaultProps

const MaanedVelger = (props: MaanedVelgerProps) => {
  const { format, placeholder, value, onChange, label } = props
  const [datePicker, setDatepicker] = useState<DatePicker>()
  const openDatePicker = useMemo(() => {
    return () => datePicker?.setOpen(true)
  }, [datePicker])

  return (
    <>
      <DatePickerLabel onClick={openDatePicker}>{label}</DatePickerLabel>
      <Datovelger>
        <div style={{ textTransform: 'capitalize' }}>
          <DatePicker
            ref={(dp) => {
              if (dp) setDatepicker(dp)
            }}
            popperProps={{
              positionFixed: true,
            }}
            dateFormat={format}
            portalId="root-portal"
            placeholderText={placeholder}
            selected={value}
            locale="nb"
            onChange={(date: Date) => onChange(date)}
            autoComplete="off"
            showMonthYearPicker
          />
        </div>
        <KalenderIkon
          tabIndex={0}
          role="button"
          title="Åpne datovelger"
          aria-label="Åpne datovelger"
          onKeyPress={openDatePicker}
          onClick={openDatePicker}
        >
          <Calender color="white" />
        </KalenderIkon>
      </Datovelger>
    </>
  )
}

MaanedVelger.defaultProps = maanedVelgerDefaultProps

const KalenderIkon = styled.div`
  padding: 4px 10px;
  cursor: pointer;
  background-color: #0167c5;
  border: 1px solid #000;
  border-radius: 0 4px 4px 0;
  height: 48px;
  line-height: 42px;
`
const DatePickerLabel = styled(Label)`
  margin-top: 12px;
`

const Datovelger = styled.div`
  display: flex;
  align-items: flex-end;
  margin-bottom: 12px;

  input {
    border-right: none;
    border-radius: 4px 0 0 4px;
    height: 48px;
    text-indent: 4px;
  }
`

export default MaanedVelger
