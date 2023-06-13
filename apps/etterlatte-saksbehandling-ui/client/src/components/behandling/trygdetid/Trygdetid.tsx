import React, { useEffect, useState } from 'react'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import {
  hentAlleLand,
  hentTrygdetid,
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlagType,
  opprettTrygdetid,
} from '~shared/api/trygdetid'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrygdetidBeregnet } from '~components/behandling/trygdetid/TrygdetidBeregnet'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { BodyShort } from '@navikt/ds-react'
import { useParams } from 'react-router-dom'
import { Grunnlagopplysninger } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { TrygdetidGrunnlagListe } from '~components/behandling/trygdetid/TrygdetidGrunnlagListe'

interface Props {
  redigerbar: boolean
}

export const Trygdetid: React.FC<Props> = ({ redigerbar }) => {
  const { behandlingId } = useParams()
  const [hentTrygdetidRequest, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [opprettTrygdetidRequest, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>()
  const [landListe, setLandListe] = useState<ILand[]>()

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

  useEffect(() => {
    fetchAlleLand(null, (landListe: ILand[]) => {
      setLandListe(
        landListe.sort((a: ILand, b: ILand) => {
          if (a.beskrivelse.tekst > b.beskrivelse.tekst) {
            return 1
          }
          return -1
        })
      )
    })
  }, [])

  return (
    <TrygdetidWrapper>
      <LovtekstMedLenke
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
      </LovtekstMedLenke>

      {trygdetid && landListe && (
        <>
          <Grunnlagopplysninger opplysninger={trygdetid.opplysninger} />

          <TrygdetidGrunnlagListe
            trygdetid={trygdetid}
            setTrygdetid={setTrygdetid}
            landListe={landListe}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
            redigerbar={redigerbar}
          />
          <TrygdetidGrunnlagListe
            trygdetid={trygdetid}
            setTrygdetid={setTrygdetid}
            landListe={landListe.filter((land) => land.isoLandkode == 'NOR')}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
            redigerbar={redigerbar}
          />
          <TrygdetidBeregnet trygdetid={trygdetid} setTrygdetid={setTrygdetid} />
        </>
      )}
      {(isPending(hentTrygdetidRequest) || isPending(hentAlleLandRequest)) && (
        <Spinner visible={true} label={'Henter trygdetid'} />
      )}
      {isPending(opprettTrygdetidRequest) && <Spinner visible={true} label={'Oppretter trygdetid'} />}
      {isFailure(hentTrygdetidRequest) && <ApiErrorAlert>En feil har oppstått ved henting av trygdetid</ApiErrorAlert>}
      {isFailure(opprettTrygdetidRequest) && (
        <ApiErrorAlert>En feil har oppstått ved opprettelse av trygdetid</ApiErrorAlert>
      )}
      {isFailure(hentAlleLandRequest) && <ApiErrorAlert>Hent feil har oppstått ved henting av landliste</ApiErrorAlert>}
    </TrygdetidWrapper>
  )
}
const TrygdetidWrapper = styled.div`
  padding: 0 4em;
  max-width: 69em;
`
