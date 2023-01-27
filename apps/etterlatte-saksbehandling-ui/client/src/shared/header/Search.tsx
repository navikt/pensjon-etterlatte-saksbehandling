import styled from 'styled-components'
import { Loader, Search as SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPerson, INVALID_FNR } from '../api/person'
import { ErrorColored, InformationColored, People } from '@navikt/ds-icons'
import { isFailure, isSuccess, isPending, useApiCall } from '~shared/hooks/useApiCall'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [feilInput, setFeilInput] = useState(false)
  const [personStatus, hentPerson, reset] = useApiCall(getPerson)

  const ugyldigInput = INVALID_FNR(searchInput)

  const soekEtterPerson = () => (ugyldigInput ? setFeilInput(true) : hentPerson(searchInput))
  const onEnter = (e: any) => e.key === 'Enter' && soekEtterPerson()

  useEffect(() => {
    if (searchInput.length === 0) {
      setFeilInput(false)
    } else if (feilInput && ugyldigInput) {
      setFeilInput(true)
    } else {
      setFeilInput(false)
    }
  }, [searchInput])

  const goToPerson = () => {
    navigate(`/person/${searchInput}`)
    reset()
  }

  return (
    <SearchWrapper>
      <SearchField
        placeholder="Fødselsnummer"
        label="Tast inn fødselsnummer"
        hideLabel
        onChange={setSearchInput}
        onKeyUp={onEnter}
        autoComplete="off"
      >
        <SearchField.Button onClick={soekEtterPerson} />
      </SearchField>

      {isPending(personStatus) && (
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
            <InformationColored />
          </span>
          <SearchResult>
            <div className="text">Tast inn gyldig fødselsnummer</div>
          </SearchResult>
        </Dropdown>
      )}

      {isSuccess(personStatus) && !feilInput && (
        <Dropdown>
          <span className="icon">
            <People />
          </span>
          <SearchResult link={true} onClick={goToPerson}>
            <div className="text">
              {personStatus.data.fornavn} {personStatus.data.etternavn}
            </div>
          </SearchResult>
        </Dropdown>
      )}

      {isFailure(personStatus) && (
        <Dropdown error={true}>
          <span className="icon">
            <ErrorColored />
          </span>
          <SearchResult>
            <div className="text">
              {personStatus.error.statusCode === 404 ? 'Fant ingen data i Doffen' : 'En feil har skjedd'}
            </div>
          </SearchResult>
        </Dropdown>
      )}
    </SearchWrapper>
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

const SearchWrapper = styled.span`
  max-width: 100%;
  padding: 0.3em;
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
