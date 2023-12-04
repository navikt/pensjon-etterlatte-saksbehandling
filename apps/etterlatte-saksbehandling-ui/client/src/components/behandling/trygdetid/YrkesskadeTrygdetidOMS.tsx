import { BodyShort } from '@navikt/ds-react'
import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import Spinner from '~shared/Spinner'
import { hentTrygdetid, ITrygdetid, lagreYrkesskadeTrygdetidGrunnlag, opprettTrygdetid } from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const YrkesskadeTrygdetid = () => {
  const { behandlingId } = useParams()
  const [hentTrygdetidRequest, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [opprettTrygdetidRequest, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [opprettYrkesskadeTrygdetidGrunnlag, requestOpprettYrkesskadeTrygdetidGrunnlag] = useApiCall(
    lagreYrkesskadeTrygdetidGrunnlag
  )

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    fetchTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
      if (trygdetid === null) {
        requestOpprettTrygdetid(behandlingId, () => {
          requestOpprettYrkesskadeTrygdetidGrunnlag({ behandlingId })
        })
      } else {
        // Må skrives om når vi gjør om til å støtte utenlands og poeng i inn/ut år (relatert til prorata)
        // Pt sjekker vi regelResultat string i backend (Trygdetid.kt) for å gjøre en "er dette yrkesskade"
        // I mellomtid så vil den bare erstatte en fast 40 år med en ny fast 40 år på samme grunnlag
        requestOpprettYrkesskadeTrygdetidGrunnlag({ behandlingId })
      }
    })
  }, [])

  return (
    <TrygdetidWrapper>
      <LovtekstMedLenke
        tittel="Trygdetid"
        hjemler={[
          {
            tittel: '§ 17-12.Pensjon etter dødsfall som skyldes yrkesskade',
            lenke: 'https://lovdata.no/lov/1997-02-28-19/§17-12',
          },
        ]}
        status={null}
      >
        <TrygdetidInfo>
          <BodyShort>Dødsfall som skyldes en skade eller sykdom som går inn under kapittel 13</BodyShort>
          <BodyShort>
            Trygdetid: <strong>40 år</strong>
          </BodyShort>
        </TrygdetidInfo>
      </LovtekstMedLenke>

      {isPending(hentTrygdetidRequest) && <Spinner visible={true} label="Henter trygdetid" />}
      {isPending(opprettTrygdetidRequest) && <Spinner visible={true} label="Oppretter trygdetid" />}
      {isPending(opprettYrkesskadeTrygdetidGrunnlag) && (
        <Spinner visible={true} label="Oppretter trygdetid grunnlag for yrkesskade" />
      )}
      {isFailureHandler({
        apiResult: hentTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved henting av trygdetid',
      })}
      {isFailureHandler({
        apiResult: opprettTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved opprettelse av trygdetid',
      })}
      {isFailureHandler({
        apiResult: opprettYrkesskadeTrygdetidGrunnlag,
        errorMessage: 'En feil har oppstått ved opprettelse av trygdetid grunnlag for yrkesskade',
      })}
    </TrygdetidWrapper>
  )
}

const TrygdetidWrapper = styled.form`
  padding: 0em 4em;
  max-width: 56em;
`

const TrygdetidInfo = styled.div`
  display: flex;
  flex-direction: column;
`

export default YrkesskadeTrygdetid
