import styled from 'styled-components'
import { BodyShort, Loader, Search as SearchField } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ABlue500, AGray900, ANavRed } from '@navikt/ds-tokens/dist/tokens'
import { InformationSquareIcon, XMarkOctagonIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fnrErGyldig } from '~utils/fnr'
import { hentSak } from '~shared/api/behandling'

import { isPending, mapFailure } from '~shared/api/apiUtils'

export const Search = () => {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [feilInput, setFeilInput] = useState(false)
  const [funnetSak, finnSak, resetSakSoek] = useApiCall(hentSak)

  const gyldigInputFnr = fnrErGyldig(searchInput)
  const gyldigInputSakId = /^\d{1,10}$/.test(searchInput ?? '')

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
          <SpinnerContent>
            <Loader />
            <span>Søker...</span>
          </SpinnerContent>
        </Dropdown>
      )}

      {feilInput && (
        <Dropdown $info={true}>
          <span className="icon">
            <InformationSquareIcon stroke={ABlue500} fill={ABlue500} />
          </span>
          <SearchResult>
            <BodyShort className="text">Tast inn gyldig fødselsnummer eller saksid</BodyShort>
          </SearchResult>
        </Dropdown>
      )}

      {mapFailure(funnetSak, (error) => (
        <Dropdown $error={true}>
          <span className="icon">
            <XMarkOctagonIcon color={ANavRed} fill={AGray900} />
          </span>
          <SearchResult>
            <BodyShort className="text">
              {error.status === 404 ? `Fant ingen sak med id ${searchInput}` : 'En feil har skjedd'}
            </BodyShort>
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

const SpinnerContent = styled.div`
  display: flex;
  gap: 0.5em;
  margin: 1em;
`
