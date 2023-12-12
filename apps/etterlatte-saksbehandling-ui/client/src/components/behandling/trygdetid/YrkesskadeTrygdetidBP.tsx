import { BodyShort, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import Spinner from '~shared/Spinner'
import { hentTrygdetid, ITrygdetid, lagreYrkesskadeTrygdetidGrunnlag, opprettTrygdetid } from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingErIverksattEllerSamordnet } from '~components/behandling/felles/utils'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

const YrkesskadeTrygdetidBP = ({ status }: { status: IBehandlingStatus }) => {
  const { behandlingId } = useParams()
  const [hentTrygdetidRequest, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [opprettTrygdetidRequest, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [opprettYrkesskadeTrygdetidGrunnlag, requestOpprettYrkesskadeTrygdetidGrunnlag] = useApiCall(
    lagreYrkesskadeTrygdetidGrunnlag
  )

  const [harPilotTrygdetid, setHarPilotTrygdetid] = useState<boolean>(false)

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    fetchTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
      if (trygdetid === null) {
        if (behandlingErIverksattEllerSamordnet(status)) {
          setHarPilotTrygdetid(true)
          return
        }
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

  if (harPilotTrygdetid) {
    return (
      <TrygdetidWrapper>
        <Heading size="small" level="3">
          Personen har fått 40 års trygdetid
        </Heading>
        <BodyShort>Denne søknaden ble satt automatisk til 40 års trygdetid</BodyShort>
      </TrygdetidWrapper>
    )
  }

  return (
    <TrygdetidWrapper>
      <LovtekstMedLenke
        tittel="Trygdetid"
        hjemler={[
          {
            tittel: '§ 18-11.Barnepensjon etter dødsfall som skyldes yrkesskade',
            lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-11',
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

export default YrkesskadeTrygdetidBP
