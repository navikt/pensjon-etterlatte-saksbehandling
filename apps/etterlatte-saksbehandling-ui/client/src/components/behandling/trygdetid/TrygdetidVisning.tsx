import { SakType } from '~shared/types/sak'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import React, { useEffect, useState } from 'react'
import FastTrygdetid from '~components/behandling/trygdetid/FastTrygdetid'
import YrkesskadeTrygdetidBP from '~components/behandling/trygdetid/YrkesskadeTrygdetidBP'
import YrkesskadeTrygdetidOMS from '~components/behandling/trygdetid/YrkesskadeTrygdetidOMS'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { oppdaterStatus } from '~shared/api/trygdetid'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const featureToggleNameTrygdetid = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid' as const

const TrygdetidVisning = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const dispatch = useAppDispatch()
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const { next } = useBehandlingRoutes()
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)

  const beregnTrygdetid = useFeatureEnabledMedDefault(featureToggleNameTrygdetid, false)
  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'
  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const [oppdaterStatusResult, oppdaterStatusRequest] = useApiCall(oppdaterStatus)

  useEffect(() => {
    getVilkaarsvurdering(behandling.id, (vurdering) => {
      setYrkesskadeTrygdetid(vurdering.isYrkesskade)
    })
  }, [])

  const virkningstidspunktEtterNyRegelDato = () => {
    return (
      behandling.virkningstidspunkt == null || new Date(behandling.virkningstidspunkt.dato) >= new Date('2024-01-01')
    )
  }

  const sjekkGyldighetOgOppdaterStatus = () => {
    oppdaterStatusRequest(behandling.id, (result) => {
      if (result.statusOppdatert) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
      }
      next()
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Trygdetid
          </Heading>
          <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
        </HeadingWrapper>
      </ContentHeader>
      {isSuccess(vilkaarsvurdering) &&
        (yrkesskadeTrygdetid ? (
          {
            [SakType.BARNEPENSJON]: <YrkesskadeTrygdetidBP />,
            [SakType.OMSTILLINGSSTOENAD]: <YrkesskadeTrygdetidOMS />,
          }[behandling.sakType]
        ) : beregnTrygdetid ? (
          <Trygdetid
            redigerbar={redigerbar}
            behandling={behandling}
            virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato()}
          />
        ) : (
          <FastTrygdetid />
        ))}

      <Border />
      {isFailureHandler({
        apiResult: vilkaarsvurdering,
        errorMessage: 'Kunne ikke hente vilkårsvurdering',
      })}
      {isFailureHandler({
        apiResult: oppdaterStatusResult,
        errorMessage: 'Kunne ikke oppdatere vilkårsvurderingsresultat',
      })}

      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" loading={isPending(oppdaterStatusResult)} onClick={sjekkGyldighetOgOppdaterStatus}>
            {handlinger.NESTE.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

export default TrygdetidVisning
