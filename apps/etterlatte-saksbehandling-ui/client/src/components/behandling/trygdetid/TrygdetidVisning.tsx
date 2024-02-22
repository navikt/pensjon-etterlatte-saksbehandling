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
import YrkesskadeTrygdetid from '~components/behandling/trygdetid/YrkesskadeTrygdetid'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { oppdaterStatus } from '~shared/api/trygdetid'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const TrygdetidVisning = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang
  const { next } = useBehandlingRoutes()
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)

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
            [SakType.BARNEPENSJON]: (
              <YrkesskadeTrygdetid
                status={behandling.status}
                hjemmel={{
                  tittel: '§ 18-11.Barnepensjon etter dødsfall som skyldes yrkesskade',
                  lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-11',
                }}
              />
            ),
            [SakType.OMSTILLINGSSTOENAD]: (
              <YrkesskadeTrygdetid
                status={behandling.status}
                hjemmel={{
                  tittel: '§ 17-12.Pensjon etter dødsfall som skyldes yrkesskade',
                  lenke: 'https://lovdata.no/lov/1997-02-28-19/§17-12',
                }}
              />
            ),
          }[behandling.sakType]
        ) : (
          <Trygdetid
            redigerbar={redigerbar}
            behandling={behandling}
            virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato()}
          />
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
