import React, { useEffect, useState } from 'react'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetid, ITrygdetid, ITrygdetidGrunnlagType, opprettTrygdetid } from '~shared/api/trygdetid'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrygdetidBeregnet } from '~components/behandling/trygdetid/TrygdetidBeregnet'
import { Soeknadsvurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsVurdering'
import styled from 'styled-components'
import { BodyShort } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { useParams } from 'react-router-dom'

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
        <BodyShort>
          Faktisk trygdetid er den tiden fra avdøde fylte 16 år til personen døde. fremtidig trygdetid er tiden fra
          dødsfallet til og med kalenderåret avdøde hadde blitt 66 år. Tilsammen kan man ha maks 40 år med trygdetid.
        </BodyShort>
      </Soeknadsvurdering>

      {trygdetid && (
        <>
          <InfoWrapper>
            <Info
              label="Fødselsdato"
              tekst={formaterStringDato(trygdetid.opplysninger.avdoedFoedselsdato.toString())}
            />
            <Info label="Dødsdato" tekst={formaterStringDato(trygdetid.opplysninger.avdoedDoedsdato.toString())} />
          </InfoWrapper>
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

export const InfoWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 2em 0 2em 0;
`
