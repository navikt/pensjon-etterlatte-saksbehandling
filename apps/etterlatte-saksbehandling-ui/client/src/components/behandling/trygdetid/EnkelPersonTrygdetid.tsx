import { Grunnlagopplysninger } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { YrkesskadeTrygdetid } from '~components/behandling/trygdetid/YrkesskadeTrygdetid'
import { TrygdetidGrunnlagListe } from '~components/behandling/trygdetid/TrygdetidGrunnlagListe'
import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlagType,
  overstyrTrygdetid,
  setTrygdetidYrkesskade,
} from '~shared/api/trygdetid'
import { OverstyrtTrygdetid } from '~components/behandling/trygdetid/OverstyrtTrygdetid'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { TrygdetidDetaljer } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'
import React, { useState } from 'react'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrygdetidManueltOverstyrt } from '~components/behandling/trygdetid/TrygdetidManueltOverstyrt'
import styled from 'styled-components'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: Boolean
  landListe: ILand[]
  fetchTrygdetider: (behandlingId: string) => void
}

export const EnkelPersonTrygdetid = (props: Props) => {
  const dispatch = useAppDispatch()
  const { redigerbar, behandling, virkningstidspunktEtterNyRegelDato, landListe, fetchTrygdetider } = props
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>(props.trygdetid)
  const [overstyrTrygdetidRequest, requestOverstyrTrygdetid] = useApiCall(overstyrTrygdetid)
  const [oppdaterYrkesskadeRequest, requestOppdaterYrkesskade] = useApiCall(setTrygdetidYrkesskade)

  const oppdaterTrygdetid = (trygdetid: ITrygdetid) => {
    setTrygdetid(trygdetid)
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
    fetchTrygdetider(behandling.id)
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

  const oppdaterYrkesskade = (yrkesskade: boolean) => {
    requestOppdaterYrkesskade(
      {
        id: trygdetid.id,
        behandlingId: trygdetid.behandlingId,
        yrkesskade: yrkesskade,
      },
      (trygdetid: ITrygdetid) => {
        oppdaterTrygdetid(trygdetid)
      }
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
    <>
      <Grunnlagopplysninger trygdetid={trygdetid} onOppdatert={oppdaterTrygdetid} redigerbar={redigerbar} />

      <YrkesskadeTrygdetid redigerbar={redigerbar} trygdetid={trygdetid} oppdaterYrkesskade={oppdaterYrkesskade} />

      <TrygdetidGrunnlagListe
        trygdetid={trygdetid}
        setTrygdetid={oppdaterTrygdetid}
        landListe={landListe}
        trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
        redigerbar={redigerbar}
      />
      <TrygdetidGrunnlagListe
        trygdetid={trygdetid}
        setTrygdetid={oppdaterTrygdetid}
        landListe={landListe.filter((land) => land.isoLandkode == 'NOR')}
        trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
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
      {isPending(oppdaterYrkesskadeRequest) && <Spinner visible={true} label="Oppdater yrkesskade" />}
      {isFailureHandler({
        apiResult: overstyrTrygdetidRequest,
        errorMessage: 'En feil har oppstått ved lagring av norsk poengår',
      })}
      {isFailureHandler({
        apiResult: oppdaterYrkesskadeRequest,
        errorMessage: 'En feil har oppstått ved oppdatering av yrkesskade',
      })}
      {trygdetid.beregnetTrygdetid && <TrygdetidDetaljer beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />}
    </>
  )
}

const TrygdetidWrapper = styled.div`
  padding: 0 4em;
  max-width: 69em;
`
