import styled from 'styled-components'
import { SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { SearchIcon } from '../icons/searchIcon'
import { useNavigate } from 'react-router-dom'
import { getPerson, IPersonResult } from '../api/person'



export const Search = () => {
  const [searchInput, setSearchInput] = useState('')
  const [searchResult, setSearchResult] = useState<IPersonResult | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    (async () => {
      if (searchInput.length === 11) {
        const personResult: any = await getPerson(searchInput)
        const person = personResult.data;
        setSearchResult(person)
      }
    })()
  }, [searchInput])

  const onChange = (e: any) => {
    setSearchInput(e.target.value)
  }

  const goToPerson = () => {
    navigate(`/person/${searchInput}`)
    setSearchResult(null)
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      goToPerson()
    }
  }

  return (
    <>
      <SearchField label="" style={{ paddingBottom: '8px' }}>
        <SearchField.Input type="tel" placeholder="Fødselsnummer" onKeyUp={onEnter} onChange={onChange} />
        <SearchField.Button>
          <SearchIcon />
        </SearchField.Button>
      </SearchField>
      {searchResult && (
        <Dropdown>
          <div onClick={goToPerson}>
            <div>{searchResult.person.fornavn} {searchResult.person.etternavn} ({searchResult.person.ident})</div>
            {searchResult.saker.saker.length === 0 ? (
              <div>Ingen fagsak. Trykk for å opprette {'->'}</div>
            ): (
              <div>Sak {console.log(searchResult.saker)} {searchResult.saker.saker[0].sakType}</div>
            )}
          </div>
        </Dropdown>
      )}
      
    </>
  )
}

const Dropdown = styled.div`
  background-color: #fff;
  width: 239px;
  height: 100px;
  top: 53px;
  border: 1px solid #000;
  position: absolute;
  color: #000;
`
