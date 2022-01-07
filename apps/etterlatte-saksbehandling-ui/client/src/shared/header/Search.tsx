import { SearchField } from '@navikt/ds-react'
import { useState } from 'react'
import { SearchIcon } from '../icons/searchIcon'
import { useNavigate } from 'react-router-dom';


export const Search = () => {
  const [searchInput, setSearchInput] = useState('');
  const navigate = useNavigate();

  const onChange = (e: any) => {
    setSearchInput(e.target.value)
  }

  const submit = () => {
    navigate(`/person/${searchInput}`)
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      submit()
    }
  }

  return (
    <SearchField label="" style={{ paddingBottom: '8px' }}>
      <SearchField.Input type="tel" placeholder="Fødselsnummer" onKeyUp={onEnter} onChange={onChange} />
      <SearchField.Button onClick={submit}>
        <SearchIcon />
      </SearchField.Button>
    </SearchField>
  )
}
