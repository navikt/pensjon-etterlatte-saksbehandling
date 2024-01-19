import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  hentAlleLand,
  hentTrygdetid,
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlagType,
  opprettTrygdetid,
  overstyrTrygdetid,
  sorterLand,
} from '~shared/api/trygdetid'
import Spinner from '~shared/Spinner'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { BodyShort, Heading } from '@navikt/ds-react'
import { Grunnlagopplysninger } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { TrygdetidGrunnlagListe } from '~components/behandling/trygdetid/TrygdetidGrunnlagListe'
import { TrygdeAvtale } from './avtaler/TrygdeAvtale'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { TrygdetidDetaljer } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'
import { OverstyrtTrygdetid } from './OverstyrtTrygdetid'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { TrygdetidManueltOverstyrt } from '~components/behandling/trygdetid/TrygdetidManueltOverstyrt'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingErIverksattEllerSamordnet } from '~components/behandling/felles/utils'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  virkningstidspunktEtterNyRegelDato: Boolean
}

const visTrydeavtale = (behandling: IDetaljertBehandling): Boolean => {
  return (
    behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale ||
    (behandling.behandlingType === IBehandlingsType.REVURDERING &&
      behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND)
  )
}

export const Trygdetid = ({ redigerbar, behandling, virkningstidspunktEtterNyRegelDato }: Props) => {
  const dispatch = useAppDispatch()
  const [hentTrygdetidRequest, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [opprettTrygdetidRequest, requestOpprettTrygdetid] = useApiCall(opprettTrygdetid)
  const [overstyrTrygdetidRequest, requestOverstyrTrygdetid] = useApiCall(overstyrTrygdetid)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>()
  const [landListe, setLandListe] = useState<ILand[]>()
  const [harPilotTrygdetid, setHarPilotTrygdetid] = useState<boolean>(false)

  const oppdaterTrygdetid = (trygdetid: ITrygdetid) => {
    setTrygdetid(trygdetid)
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
  }

  const overstyrTrygdetidPoengaar = (trygdetid: ITrygdetid) => {
    requestOverstyrTrygdetid(
      {
        id: trygdetid.id,
        behandlingId: trygdetid.behandlingId,
        overstyrtNorskPoengaar: trygdetid.overstyrtNorskPoengaar,
      },
      (trygdetid: ITrygdetid) => {
        oppdaterTrygdetid(trygdetid)
      }
    )
  }

  useEffect(() => {
    if (!behandling?.id) throw new Error('Mangler behandlingsid')

    fetchTrygdetid(behandling.id, (trygdetid: ITrygdetid) => {
      if (trygdetid === null) {
        if (behandlingErIverksattEllerSamordnet(behandling.status)) {
          setHarPilotTrygdetid(true)
        } else {
          requestOpprettTrygdetid(behandling.id, (trygdetid: ITrygdetid) => {
            oppdaterTrygdetid(trygdetid)
          })
        }
      } else {
        setTrygdetid(trygdetid)
      }
    })
  }, [])

  useEffect(() => {
    fetchAlleLand(null, (landListe: ILand[]) => {
      setLandListe(sorterLand(landListe))
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

  if (trygdetid?.beregnetTrygdetid?.resultat.overstyrt) {
    return (
      <TrygdetidWrapper>
        <TrygdetidManueltOverstyrt
          behandlingId={behandling.id}
          oppdaterTrygdetid={oppdaterTrygdetid}
          beregnetTrygdetid={trygdetid.beregnetTrygdetid}
        />
        <TrygdetidDetaljer beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
      </TrygdetidWrapper>
    )
  }

  return (
    <TrygdetidWrapper>
      {visTrydeavtale(behandling) && <TrygdeAvtale redigerbar={redigerbar} />}
      <LovtekstMedLenke
        tittel="Avdødes trygdetid"
        hjemler={[
          {
            tittel: '§ 3-5 Trygdetid ved beregning av ytelser',
            lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§3-5',
          },
          {
            tittel: 'EØS-forordning 883/2004 artikkel 52',
            lenke: 'https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_52',
          },
        ]}
        status={null}
      >
        <BodyShort>
          Faktisk trygdetid kan gis fra avdøde fylte 16 år til dødsfall. Hadde avdøde opptjent pensjonspoeng fra fylte
          67 år til og med 75 år, gis det også et helt års trygdetid for aktuelle poengår. Fremtidig trygdetid kan gis
          fra dødsfallet til og med kalenderåret avdøde hadde blitt 66 år. Trygdetiden beregnes med maks 40 år. Avdødes
          utenlandske trygdetid fra avtaleland skal legges til for alternativ prorata-beregning av ytelsen. Ulike
          avtaler skal ikke beregnes sammen.
        </BodyShort>
      </LovtekstMedLenke>
      {trygdetid && landListe && (
        <>
          <Grunnlagopplysninger trygdetid={trygdetid} onOppdatert={oppdaterTrygdetid} redigerbar={redigerbar} />

          <TrygdetidGrunnlagListe
            trygdetid={trygdetid}
            setTrygdetid={oppdaterTrygdetid}
            landListe={landListe}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
            redigerbar={redigerbar}
          />
          <OverstyrtTrygdetid
            redigerbar={redigerbar}
            sakType={behandling.sakType}
            trygdetid={trygdetid}
            overstyrTrygdetidPoengaar={overstyrTrygdetidPoengaar}
            virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
          />
          {isPending(overstyrTrygdetidRequest) && <Spinner visible={true} label="Oppdatere poengår" />}
          <TrygdetidGrunnlagListe
            trygdetid={trygdetid}
            setTrygdetid={oppdaterTrygdetid}
            landListe={landListe.filter((land) => land.isoLandkode == 'NOR')}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
            redigerbar={redigerbar}
          />
          {trygdetid.beregnetTrygdetid && (
            <TrygdetidDetaljer beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
          )}
        </>
      )}
      {(isPending(hentTrygdetidRequest) || isPending(hentAlleLandRequest)) && (
        <Spinner visible={true} label="Henter trygdetid" />
      )}
      {isPending(opprettTrygdetidRequest) && <Spinner visible={true} label="Oppretter trygdetid" />}
      {isFailureHandler({
        apiResult: hentTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved henting av trygdetid',
      })}
      {isFailureHandler({
        apiResult: overstyrTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved lagring av norsk poengår',
      })}
      {isFailureHandler({
        apiResult: opprettTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved opprettelse av trygdetid',
      })}
      {isFailureHandler({
        apiResult: hentAlleLandRequest,
        errorMessage: 'Hent feil har oppstått ved henting av landliste',
      })}
    </TrygdetidWrapper>
  )
}
const TrygdetidWrapper = styled.div`
  padding: 0 4em;
  max-width: 69em;
`
