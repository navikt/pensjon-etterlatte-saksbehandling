import React from 'react'
import styled from 'styled-components'
import { MonthPicker, useMonthpicker } from '@navikt/ds-react'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'

type MaanedVelgerProps = {
  onChange: (date: Date | null) => void
  value?: Date
  label: string
}

const MaanedVelger = (props: MaanedVelgerProps) => {
  const { value, onChange, label } = props

  const { monthpickerProps, inputProps } = useMonthpicker({
    onMonthChange: (date: Date) => onChange(date),
    defaultSelected: value ?? undefined,
    locale: 'nb',
  } as UseMonthPickerOptions)

  return (
    <MonthPickerWrapper>
      <MonthPicker {...monthpickerProps}>
        <MonthPicker.Input label={label} {...inputProps} />
      </MonthPicker>
    </MonthPickerWrapper>
  )
}

const MonthPickerWrapper = styled.div`
  margin-bottom: 12px;
`

export default MaanedVelger
