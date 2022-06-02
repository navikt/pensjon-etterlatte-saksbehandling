import styled from 'styled-components'
import { SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { SearchIcon } from '../icons/searchIcon'
import { useNavigate } from 'react-router-dom'
import { getPerson, IPersonResult, opprettSakPaaPerson } from '../api/person'
import { ErrorIcon } from '../icons/errorIcon'
import { InformationIcon } from '../icons/informationIcon'
import { PeopleIcon } from '../icons/peopleIcon'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [searchResult, setSearchResult] = useState<IPersonResult | null>(null)
  const regBokstaver = /[a-zA-Z]/g
  const [feilInput, setFeilInput] = useState(false)

  useEffect(() => {
    (async () => {
      if (regBokstaver.test(searchInput) || searchInput.length > 11) {
        setFeilInput(true)
        setSearchResult(null)
      } else {
        setFeilInput(false)
      }
      if (searchInput.length === 11) {
        const personResult: any = await getPerson(searchInput)
        const person = personResult.data;
        setSearchResult(person)
      } else if (searchInput.length < 11) {
        setSearchResult(null)
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
      {searchResult && !feilInput && (
        <Dropdown>
          <span className="icon">
            <PeopleIcon />
          </span>
          <SearchResult onClick={goToPerson}>
            <div className="text">
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
      {feilInput && (
        <Dropdown info={true}>
          <span className="icon">
            <InformationIcon />
          </span>
          <SearchResult>
            <div className="text">Tast inn fødselsnummer</div>
          </SearchResult>
        </Dropdown>
      )}

      {searchResult === undefined && (
        <Dropdown error={true}>
          <span className="icon">
            <ErrorIcon />
          </span>
          <SearchResult>
            <div className="text">En feil har oppstått.</div>
          </SearchResult>
        </Dropdown>
      )}
    </>
  )
}

const Dropdown = styled.div<{ error?: boolean; info?: boolean }>`
  display: flex;
  background-color: ${(props) => (props.error ? '#f9d2cc' : props.info ? '#cce1ff' : '#fff')};
  width: 300px;
  height: fit-content;
  top: 53px;
  border: 1px solid ${(props) => (props.error ? '#ba3a26' : props.info ? '#368da8' : '#000')};
  position: absolute;
  color: #000;

  .icon {
    margin: 20px;
    margin-right: 0px;
    align-self: center;
  }
`

const SearchResult = styled.div`
  cursor: pointer;
  padding: 0.5em;
  padding-left: 1em;
  align-self: center;

  .text {
    font-weight: 500;
    font-size: 20px;
  }

  .sak {
    color: gray;
  }
`
