import styled from 'styled-components'
import { BodyShort, Search as SearchField } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { InformationSquareIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fnrErGyldig } from '~utils/fnr'
import { hentSak } from '~shared/api/behandling'

import { isPending, mapFailure } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiError } from '~shared/api/apiClient'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [feilInput, setFeilInput] = useState(false)
  const [funnetSak, finnSak, resetSakSoek] = useApiCall(hentSak)

  const gyldigInputFnr = fnrErGyldig(searchInput)
  const gyldigInputSakId = /^\d{1,10}$/.test(searchInput ?? '')

  /*
    TODO: EY-5128
    Denne kan potensielt gjøre et kall for å sjekke om person er skjermet
    slik at man ikke får navigert til saken.
    Potensielt viser man ting man ikke skal om man får navigert til saksoversikten.
    navn-foedsel i pdltjenester kan verifisere dette.
   */
  const avgjoerSoek = () => {
    if (gyldigInputFnr) {
      navigate('/person', { state: { fnr: searchInput } })
      return
    }
    if (gyldigInputSakId) {
      finnSak(searchInput, (sak) => {
        navigate('/person', { state: { fnr: sak.ident } })
      })
      return
    }
  }

  const onEnter = (e: any) => {
    if (e.key === 'Enter') {
      avgjoerSoek()
    }
  }

  useEffect(() => {
    resetSakSoek()
    setFeilInput(!!searchInput.length && !(gyldigInputFnr || gyldigInputSakId))
  }, [searchInput])

  const feilkodehaandtering = (error: ApiError) => {
    switch (error.status) {
      case 404:
        return `Fant ingen sak med id ${searchInput}`
      case 403:
        return `Du mangler tilgang til saken: ${error.detail}`
      default:
        return 'En feil har skjedd'
    }
  }

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

      {isPending(funnetSak) && (
        <Dropdown>
          <Spinner label="Søker..." />
        </Dropdown>
      )}

      {feilInput && (
        <Dropdown $info={true}>
          <span className="icon">
            <InformationSquareIcon stroke="var(--ax-accent-600)" fill="var(--ax-accent-600)" />
          </span>
          <SearchResult>
            <BodyShort className="text">Tast inn gyldig fødselsnummer eller saksid</BodyShort>
          </SearchResult>
        </Dropdown>
      )}

      {mapFailure(funnetSak, (error) => (
        <Dropdown $error={true}>
          <span className="icon">
            <XMarkOctagonIcon color="var(--ax-text-logo)" fill="var(--ax-neutral-1000)" />
          </span>
          <SearchResult>
            <BodyShort className="text">{feilkodehaandtering(error)}</BodyShort>
          </SearchResult>
        </Dropdown>
      ))}
    </SearchWrapper>
  )
}

const Dropdown = styled.div<{ $error?: boolean; $info?: boolean }>`
  display: flex;
  background-color: ${(props) => (props.$error ? '#f9d2cc' : props.$info ? '#cce1ff' : '#fff')};
  width: 300px;
  height: fit-content;
  top: 53px;
  border: 1px solid ${(props) => (props.$error ? '#ba3a26' : props.$info ? '#368da8' : '#000')};
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

const SearchResult = styled.div`
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
