import styled from 'styled-components'
import { Loader, Search as SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPerson } from '../api/person'
import { ErrorIcon } from '../icons/errorIcon'
import { InformationIcon } from '../icons/informationIcon'
import { PeopleIcon } from '../icons/peopleIcon'
import { IPersonResult } from '../../components/person/typer'
import { ApiResponse } from '../api/apiClient'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [searchResult, setSearchResult] = useState<IPersonResult | undefined>(undefined)
  const [error, setError] = useState<IPersonResult | undefined>(undefined)
  const [laster, setLaster] = useState<boolean>(false)
  const regBokstaver = /[a-zA-Z]/g
  const [feilInput, setFeilInput] = useState(false)

  function search() {
    setSearchResult(undefined)
    setError(undefined)
    if (ugyldigInput()) {
      setFeilInput(true)
    } else {
      setLaster(true)
      setFeilInput(false)
      getPerson(searchInput).then((result: ApiResponse<IPersonResult>) => {
        setLaster(false)
        if (result.status === 'ok') {
          setSearchResult(result?.data)
        } else {
          setError(result?.error)
        }
      })
    }
  }

  function ugyldigInput(): boolean {
    return regBokstaver.test(searchInput) || searchInput.length !== 11
  }

  useEffect(() => {
    ;(async () => {
      setSearchResult(undefined)
      setError(undefined)
      if (searchInput.length === 0) {
        setFeilInput(false)
      } else if (feilInput && ugyldigInput()) {
        setFeilInput(true)
      } else {
        setFeilInput(false)
      }
    })()
  }, [searchInput])

  const goToPerson = () => {
    navigate(`/person/${searchInput}`)
    setSearchResult(undefined)
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      search()
    }
  }

  return (
    <>
      <SearchField
        placeholder="Fødselsnummer"
        label="Tast inn fødselsnummer"
        hideLabel
        onChange={setSearchInput}
        onKeyUp={onEnter}
      >
        <SearchField.Button onClick={search} />
      </SearchField>

      {laster && (
        <Dropdown>
          <SpinnerContent>
            <Loader />
            <span>Søker...</span>
          </SpinnerContent>
        </Dropdown>
      )}

      {feilInput && (
        <Dropdown info={true}>
          <span className="icon">
            <InformationIcon />
          </span>
          <SearchResult>
            <div className="text">Tast inn gyldig fødselsnummer</div>
          </SearchResult>
        </Dropdown>
      )}

      {searchResult && !feilInput && (
        <Dropdown>
          <span className="icon">
            <PeopleIcon />
          </span>
          <SearchResult link={true} onClick={goToPerson}>
            <div className="text">
              {searchResult.person.fornavn} {searchResult.person.etternavn}
            </div>
          </SearchResult>
        </Dropdown>
      )}

      {error && (
        <Dropdown error={true}>
          <span className="icon">
            <ErrorIcon />
          </span>
          <SearchResult>
            <div className="text">{error}</div>
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

const SearchResult = styled.div<{ link?: boolean }>`
  cursor: ${(props) => (props.onClick ? 'pointer' : 'default')};

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

const SpinnerContent = styled.div`
  display: flex;
  gap: 0.5em;
  margin: 1em;
`
