import React, { useEffect, useState } from 'react'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetid, ITrygdetid, ITrygdetidGrunnlagType, opprettTrygdetid } from '~shared/api/trygdetid'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrygdetidBeregnet } from '~components/behandling/trygdetid/TrygdetidBeregnet'
import { Soeknadsvurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsVurdering'
import styled from 'styled-components'

export const Trygdetid = () => {
  const { behandlingId } = useParams()

  const [trygdetidStatus, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>()

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    fetchTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
      if (trygdetid == null) {
        requestOpprettTrygdetid(behandlingId, (trygdetid: ITrygdetid) => {
          setTrygdetid(trygdetid)
        })
      } else {
        setTrygdetid(trygdetid)
      }
    })
  }, [])

  return (
    <TrygdetidWrapper>
      <Soeknadsvurdering
        tittel={'Avdødes trygdetid'}
        hjemler={[
          {
            tittel: '§ 3-5 Trygdetid ved beregning av ytelser',
            lenke: 'https://lovdata.no/lov/1997-02-28-19/§3-5',
          },
        ]}
        status={null}
      >
        <TrygdetidInfo>
          <p>
            Faktisk trygdetid er den tiden fra avdøde fylte 16 år til personen døde. fremtidig trygdetid er tiden fra
            dødsfallet til og med kalenderåret avdøde hadde blitt 66 år. Tilsammen kan man ha maks 40 år med trygdetid.
          </p>
        </TrygdetidInfo>
      </Soeknadsvurdering>

      {trygdetid && (
        <>
          <TrygdetidGrunnlag
            trygdetid={trygdetid}
            setTrygdetid={setTrygdetid}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.NASJONAL}
          />
          <TrygdetidGrunnlag
            trygdetid={trygdetid}
            setTrygdetid={setTrygdetid}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
          />
          <TrygdetidBeregnet trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
        </>
      )}
      {isPending(trygdetidStatus) && <Spinner visible={true} label={'Henter trygdetid'} />}
      {isFailure(trygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
    </TrygdetidWrapper>
  )
}
const TrygdetidWrapper = styled.div`
  padding: 0 4em;
  max-width: 52em;
`

const TrygdetidInfo = styled.div`
  display: flex;
  flex-direction: column;
`
