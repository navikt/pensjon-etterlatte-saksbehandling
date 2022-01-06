import { SearchField } from '@navikt/ds-react'
import { useState } from 'react'

export const Search = () => {
  const [searchInput, setSearchInput] = useState('')

  const onChange = (e: any) => {
    setSearchInput(e.target.value)
  }

  const submit = () => {
    alert(`Du søkte på ${searchInput}`)
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      submit()
    }
  }

  return (
    <SearchField label="" style={{ paddingBottom: '8px' }}>
      <SearchField.Input type="tel" placeholder="123456 12345" onKeyUp={onEnter} onChange={onChange} />
      <SearchField.Button onClick={submit}>?</SearchField.Button>
    </SearchField>
  )
}
