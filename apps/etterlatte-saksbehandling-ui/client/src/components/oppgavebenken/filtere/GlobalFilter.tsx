import 'regenerator-runtime'
import { TextField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useAsyncDebounce } from 'react-table'
import { FilterElement, FilterWrapper } from '../styled'

export const GlobalFilter = ({
  setGlobalFilter,
  resetGlobalInput,
  setResetGlobalInput,
}: {
  setGlobalFilter: (value: string | undefined) => void
  setResetGlobalInput: (value: boolean) => void
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
        <TextField
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
