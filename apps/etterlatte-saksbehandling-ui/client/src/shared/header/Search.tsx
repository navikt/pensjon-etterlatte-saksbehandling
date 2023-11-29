import styled from 'styled-components'
import { BodyShort, Loader, Search as SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ABlue500, AGray900, ANavRed } from '@navikt/ds-tokens/dist/tokens'
import { InformationSquareIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { getPerson } from '~shared/api/grunnlag'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { ApiError } from '~shared/api/apiClient'
import { hentSak } from '~shared/api/behandling'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [feilInput, setFeilInput] = useState(false)
  const [personStatus, hentPerson, reset] = useApiCall(getPerson)
  const [funnetSak, finnSak, resetSakSoek] = useApiCall(hentSak)

  const gyldigInputFnr = fnrHarGyldigFormat(searchInput)
  const gyldigInputSakId = /^\d{1,10}$/.test(searchInput ?? '')
  const avgjoerSoek = () => {
    if (gyldigInputFnr) {
      hentPerson(searchInput)
      return
    }
    if (gyldigInputSakId) {
      finnSak(searchInput)
      return
    }
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      avgjoerSoek()
    }
  }

  useEffect(() => {
    reset()
    resetSakSoek()
    setFeilInput(!!searchInput.length && !(gyldigInputFnr || gyldigInputSakId))
  }, [searchInput])

  useEffect(() => {
    if (isSuccess(personStatus)) {
      navigate(`/person/${searchInput}`)
      return
    }
    if (isSuccess(funnetSak)) {
      navigate(`/person/${funnetSak.data.ident}`)
    }
  }, [personStatus, funnetSak])

  return (
    <SearchWrapper>
      <SearchField
        placeholder="Fødselsnummer eller sakid "
        label="Tast inn fødselsnummer eller sakid"
        hideLabel
        onChange={(value) => setSearchInput(value.trim())}
        onKeyUp={onEnter}
        autoComplete="off"
      >
        <SearchField.Button onClick={avgjoerSoek} />
      </SearchField>

      {(isPending(personStatus) || isPending(funnetSak)) && (
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
            <InformationSquareIcon stroke={ABlue500} fill={ABlue500} />
          </span>
          <SearchResult>
            <BodyShort className="text">Tast inn gyldig fødselsnummer eller saksid</BodyShort>
          </SearchResult>
        </Dropdown>
      )}

      {isFailure(funnetSak) && (
        <Dropdown error={true}>
          <span className="icon">
            <XMarkOctagonIcon color={ANavRed} fill={AGray900} />
          </span>
          <SearchResult>
            <BodyShort className="text">{feilmelding(funnetSak.error)}</BodyShort>
          </SearchResult>
        </Dropdown>
      )}
      {isFailure(personStatus) && (
        <Dropdown error={true}>
          <span className="icon">
            <XMarkOctagonIcon color={ANavRed} fill={AGray900} />
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
  if (error.status === 404) {
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
