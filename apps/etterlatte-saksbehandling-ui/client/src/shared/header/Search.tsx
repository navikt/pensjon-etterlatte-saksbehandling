import styled from 'styled-components'
import { SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { SearchIcon } from '../icons/searchIcon'
import { useNavigate } from 'react-router-dom'
import { getPerson, IPersonResult, opprettSakPaaPerson } from '../api/person'
import { People } from '@navikt/ds-icons'

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

  const opprettSak = (fnr: string) => {
    if (fnr) {
      opprettSakPaaPerson(fnr)
    }
  }

  return (
    <>
      <SearchField label="" style={{ paddingBottom: '8px', width: '300px' }}>
        <SearchField.Input type="tel" placeholder="Fødselsnummer" onKeyUp={onEnter} onChange={onChange} />
        <SearchField.Button>
          <SearchIcon />
        </SearchField.Button>
      </SearchField>
      {searchResult && (
        <Dropdown>
          <People className="icon" fontSize={25} />
          <SearchResult onClick={goToPerson}>
            <div className="navn">
              {searchResult.person.fornavn} {searchResult.person.etternavn}
            </div>
            {searchResult.saker.saker.length === 0 ? (
              <div className="sak" onClick={() => opprettSak(searchInput)}>
                Ingen fagsak. Trykk for å opprette {'>'}
              </div>
            ) : (
              <div className="sak">
                Sak {console.log(searchResult.saker)} {searchResult.saker.saker[0].sakType}
              </div>
            )}
          </SearchResult>
        </Dropdown>
      )}
    </>
  )
}

const Dropdown = styled.div`
  display: flex;
  background-color: #fff;
  width: 300px;
  height: fit-content;
  top: 53px;
  border: 1px solid #000;
  position: absolute;
  color: #000;

  .icon {
    margin-left: 10px;
    align-self: center;
  }
`

const SearchResult = styled.div`
  cursor: pointer;
  padding: 0.5em;
  padding-left: 1em;
  align-self: center;

  .navn {
    font-weight: 500;
  }

  .sak {
    color: gray;
  }
`
