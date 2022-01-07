import React, { useEffect, useState } from 'react'
import { useAsyncDebounce } from 'react-table'
import { Input } from 'nav-frontend-skjema'
import { FilterElement, FilterWrapper } from '../styled'

export const GlobalFilter = ({
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
    <FilterWrapper>
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
    </FilterWrapper>
  )
}
