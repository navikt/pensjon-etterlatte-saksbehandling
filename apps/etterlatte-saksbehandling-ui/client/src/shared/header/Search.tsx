import styled from 'styled-components'
import { BodyShort, Loader, Search as SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ErrorColored, InformationColored } from '@navikt/ds-icons'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { INVALID_FNR } from '~utils/fnr'
import { ApiError } from '~shared/api/apiClient'
import { finnSakForSoek } from '~shared/api/behandling'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [feilInput, setFeilInput] = useState(false)
  const [personStatus, hentPerson, reset] = useApiCall(getPerson)
  const [funnetFnrForSak, finnSak, resetSakSoek] = useApiCall(finnSakForSoek)

  const ugyldigInput = INVALID_FNR(searchInput)

  const soekEtterPerson = () => (ugyldigInput ? setFeilInput(true) : hentPerson(searchInput))

  const soekEtterSak = () => {
    if (searchInput && /^\d+$/.test(searchInput ?? '')) {
      finnSak(searchInput)
    }
  }
  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      soekEtterPerson()
      soekEtterSak()
    }
  }

  useEffect(() => {
    reset()
    resetSakSoek()
    if (searchInput.length === 0) {
      setFeilInput(false)
    } else if (feilInput && ugyldigInput) {
      setFeilInput(true)
    } else {
      setFeilInput(false)
    }
  }, [searchInput])

  useEffect(() => {
    if (isSuccess(personStatus)) {
      navigate(`/person/${searchInput}`)
      return
    }
    if (isSuccess(funnetFnrForSak)) {
      navigate(`/person/${funnetFnrForSak.data}`)
    }
  }, [personStatus, funnetFnrForSak])

  return (
    <SearchWrapper>
      <SearchField
        placeholder="Fødselsnummer eller sakid "
        label="Tast inn fødselsnummer eller sakid"
        hideLabel
        onChange={setSearchInput}
        onKeyUp={onEnter}
        autoComplete="off"
      >
        <SearchField.Button
          onClick={() => {
            soekEtterPerson()
            soekEtterSak()
          }}
        />
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
            <BodyShort className="text">Tast inn gyldig fødselsnummer</BodyShort>
          </SearchResult>
        </Dropdown>
      )}

      {isFailure(personStatus) && (
        <Dropdown error={true}>
          <span className="icon">
            <ErrorColored />
          </span>
          <SearchResult>
            <BodyShort className="text">{feilmelding(personStatus.error)}</BodyShort>
          </SearchResult>
        </Dropdown>
      )}
    </SearchWrapper>
  )
}

const feilmelding = (error: ApiError) => {
  if (error.statusCode === 404) {
    return 'Fant ingen data i Gjenny'
  } else {
    return 'En feil har skjedd'
  }
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
  min-width: 350px;
  padding: 0.5em;
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
