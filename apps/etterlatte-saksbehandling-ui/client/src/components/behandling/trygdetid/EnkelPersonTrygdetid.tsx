import { Grunnlagopplysninger } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { YrkesskadeTrygdetid } from '~components/behandling/trygdetid/YrkesskadeTrygdetid'
import { ITrygdetid, ITrygdetidGrunnlagType, overstyrTrygdetid, setTrygdetidYrkesskade } from '~shared/api/trygdetid'
import { OverstyrtTrygdetid } from '~components/behandling/trygdetid/OverstyrtTrygdetid'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { TrygdetidDetaljer } from '~components/behandling/trygdetid/detaljer/TrygdetidDetaljer'
import React, { useEffect, useState } from 'react'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrygdetidManueltOverstyrt } from '~components/behandling/trygdetid/TrygdetidManueltOverstyrt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { TrygdetidPerioder } from '~components/behandling/trygdetid/trygdetidPerioder/TrygdetidPerioder'
import { VStack } from '@navikt/ds-react'
import { skalViseTrygdeavtale } from '~components/behandling/trygdetid/utils'
import { AvdoedesTrygdetidReadMore } from '~components/behandling/trygdetid/components/AvdoedesTrygdetidReadMore'
import { ILand } from '~utils/kodeverk'
import { OpprettManueltOverstyrtTrygdetid } from '~components/behandling/trygdetid/OpprettManueltOverstyrtTrygdetid'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: boolean
  landListe: ILand[]
  fetchTrygdetider: (behandlingId: string) => void
}

export const EnkelPersonTrygdetid = (props: Props) => {
  const dispatch = useAppDispatch()
  const { redigerbar, behandling, virkningstidspunktEtterNyRegelDato, landListe, fetchTrygdetider } = props
  const [trygdetid, setTrygdetid] = useState<ITrygdetid | undefined>()
  const [overstyrTrygdetidRequest, requestOverstyrTrygdetid] = useApiCall(overstyrTrygdetid)
  const [oppdaterYrkesskadeRequest, requestOppdaterYrkesskade] = useApiCall(setTrygdetidYrkesskade)
  const overstyrTrygdetidFeature = useFeaturetoggle(FeatureToggle.overstyr_trygdetid_knapp)

  useEffect(() => {
    if (props.trygdetid) {
      setTrygdetid(props.trygdetid)
    }
  }, [props.trygdetid])

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
    if (trygdetid) {
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
  }

  if (trygdetid?.id && trygdetid?.beregnetTrygdetid?.resultat.overstyrt) {
    return (
      <VStack gap="12" maxWidth="69rem">
        <TrygdetidManueltOverstyrt
          trygdetidId={trygdetid.id}
          ident={trygdetid.ident}
          oppdaterTrygdetid={oppdaterTrygdetid}
          beregnetTrygdetid={trygdetid.beregnetTrygdetid}
          tidligereFamiliepleier={!!behandling.tidligereFamiliepleier?.svar}
          redigerbar={redigerbar}
        />
        <TrygdetidDetaljer beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
      </VStack>
    )
  }

  return (
    <>
      {trygdetid && (
        <VStack gap="12" maxWidth="69rem">
          {overstyrTrygdetidFeature && (
            <OpprettManueltOverstyrtTrygdetid behandlingId={behandling.id} redigerbar={redigerbar} />
          )}

          <VStack gap="4">
            {!skalViseTrygdeavtale(behandling) && <AvdoedesTrygdetidReadMore />}
            <Grunnlagopplysninger trygdetid={trygdetid} onOppdatert={oppdaterTrygdetid} redigerbar={redigerbar} />
          </VStack>

          <YrkesskadeTrygdetid redigerbar={redigerbar} trygdetid={trygdetid} oppdaterYrkesskade={oppdaterYrkesskade} />

          <TrygdetidPerioder
            trygdetid={trygdetid}
            oppdaterTrygdetid={oppdaterTrygdetid}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
            landListe={landListe}
            redigerbar={redigerbar}
          />

          <TrygdetidPerioder
            trygdetid={trygdetid}
            oppdaterTrygdetid={oppdaterTrygdetid}
            trygdetidGrunnlagType={ITrygdetidGrunnlagType.FREMTIDIG}
            landListe={landListe.filter((land) => land.isoLandkode == 'NOR')}
            redigerbar={redigerbar}
          />

          <OverstyrtTrygdetid
            redigerbar={redigerbar}
            sakType={behandling.sakType}
            trygdetid={trygdetid}
            overstyrTrygdetidPoengaar={overstyrTrygdetidPoengaar}
            virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
          />

          <Spinner label="Oppdatere poengår" visible={isPending(overstyrTrygdetidRequest)} />
          <Spinner label="Oppdater yrkesskade" visible={isPending(oppdaterYrkesskadeRequest)} />

          {isFailureHandler({
            apiResult: overstyrTrygdetidRequest,
            errorMessage: 'En feil har oppstått ved lagring av norsk poengår',
          })}
          {isFailureHandler({
            apiResult: oppdaterYrkesskadeRequest,
            errorMessage: 'En feil har oppstått ved oppdatering av yrkesskade',
          })}
          {trygdetid.beregnetTrygdetid && (
            <TrygdetidDetaljer beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
          )}
        </VStack>
      )}
    </>
  )
}
