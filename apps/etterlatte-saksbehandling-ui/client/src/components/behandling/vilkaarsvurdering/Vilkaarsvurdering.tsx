import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, opprettVilkaarsvurdering, slettVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { Resultat } from './Resultat'
import Spinner from '~shared/Spinner'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { Alert, BodyLong, Box, Button, Heading } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import {
  behandlingGjelderBarnepensjonPaaNyttRegelverk,
  vilkaarsvurderingErPaaNyttRegelverk,
} from '~components/behandling/vilkaarsvurdering/utils'

import { isFailure, isInitial, isPending, mapFailure } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { ClickEvent, trackClick } from '~utils/analytics'
import { KopierVilkaarAvdoed } from '~components/behandling/vilkaarsvurdering/KopierVilkaarAvdoed'

export const Vilkaarsvurdering = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props

  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [vilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [slettVilkaarsvurderingStatus, slettGammelVilkaarsvurdering] = useApiCall(slettVilkaarsvurdering)
  const [opprettNyVilkaarsvurderingStatus, opprettNyVilkaarsvurdering] = useApiCall(opprettVilkaarsvurdering)

  const [redigerTotalvurdering, setRedigerTotalvurdering] = useState<boolean>(false)

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    if (!vilkaarsvurdering) {
      fetchVilkaarsvurdering(behandlingId, (vilkaarsvurdering) => {
        if (vilkaarsvurdering == null) {
          createVilkaarsvurdering(behandlingId, true)
        } else {
          dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
        }
      })
    }
  }, [behandlingId])

  const createVilkaarsvurdering = (behandlingId: string, kopier: boolean) => {
    opprettNyVilkaarsvurdering({ behandlingId: behandlingId, kopierVedRevurdering: kopier }, (vilkaarsvurdering) => {
      dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
      if (behandling.behandlingType === IBehandlingsType.REVURDERING) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
    })
  }

  const visHarGammelVilkaarsvurdering = () =>
    vilkaarsvurdering &&
    redigerbar &&
    !vilkaarsvurderingErPaaNyttRegelverk(vilkaarsvurdering) &&
    behandlingGjelderBarnepensjonPaaNyttRegelverk(behandling)

  const visOppdaterteVilkaar = () =>
    vilkaarsvurdering &&
    redigerbar &&
    behandling.behandlingType == IBehandlingsType.REVURDERING &&
    vilkaarsvurdering?.resultat == undefined

  const resetVilkaarsvurdering = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    trackClick(ClickEvent.SLETT_VILKAARSVURDERING)
    slettGammelVilkaarsvurdering(behandlingId, () => {
      dispatch(updateVilkaarsvurdering(undefined))
      createVilkaarsvurdering(behandlingId, false)
    })
  }

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading size="large" level="1">
          Vilkårsvurdering
        </Heading>
      </Box>

      {behandlingId && vilkaarsvurdering && !isPending(slettVilkaarsvurderingStatus) && (
        <>
          {visHarGammelVilkaarsvurdering() && (
            <AlertWrapper>
              <Alert variant="info">
                <BodyLong>
                  Denne behandlingen har automatisk kopiert over en vilkårsvurdering fra gammelt regelverk (før
                  1.1.2024). For å få oppdaterte vilkår ihht. nytt regelverk må vilkårsvurderingen opprettes på nytt. Du
                  kan kopiere ev. begrunnelser fra tidligere behandling.
                </BodyLong>
                <Button type="button" variant="secondary" onClick={resetVilkaarsvurdering}>
                  Slett vilkårsvurdering
                </Button>
              </Alert>
              {isFailureHandler({
                apiResult: slettVilkaarsvurderingStatus,
                errorMessage: 'Klarte ikke slette vilkårsvurderingen',
              })}
            </AlertWrapper>
          )}

          {visOppdaterteVilkaar() && (
            <AlertWrapper>
              <Alert variant="info">
                <BodyLong>
                  Denne behandlingen har automatisk kopiert vilkårsvurderingen fra forrige iverksatte behandling. Et
                  eller flere vilkår er lagt til eller fjernet og utfall må derfor vurderes på nytt.
                </BodyLong>
              </Alert>
            </AlertWrapper>
          )}

          <Box paddingInline="space-16" paddingBlock="space-16 space-4">
            {behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING &&
              vilkaarsvurdering.resultat == null && (
                <KopierVilkaarAvdoed behandlingId={behandling.id} vilkaar={vilkaarsvurdering.vilkaar} />
              )}
          </Box>

          {vilkaarsvurdering.vilkaar.map((value, index) => (
            <ManueltVilkaar
              key={index}
              vilkaar={value}
              oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
              behandlingId={behandlingId}
              redigerbar={redigerbar && !vilkaarsvurdering.resultat && !redigerTotalvurdering}
            />
          ))}

          <Resultat
            setRedigerTotalvurdering={setRedigerTotalvurdering}
            redigerTotalvurdering={redigerTotalvurdering}
            virkningstidspunktDato={behandling.virkningstidspunkt?.dato}
            sakstype={behandling.sakType}
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
            behandlingId={behandlingId}
            redigerbar={redigerbar}
            behandlingstype={behandling.behandlingType}
            revurderingsaarsak={behandling.revurderingsaarsak}
          />
        </>
      )}

      <Spinner visible={isPending(vilkaarsvurderingStatus)} label="Henter vilkårsvurdering" />
      <Spinner visible={isPending(opprettNyVilkaarsvurderingStatus)} label="Oppretter vilkårsvurdering" />
      <Spinner visible={isPending(slettVilkaarsvurderingStatus)} label="Sletter vilkårsvurdering" />

      {isFailure(vilkaarsvurderingStatus) && isInitial(opprettNyVilkaarsvurderingStatus) && (
        <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
      )}
      {mapFailure(opprettNyVilkaarsvurderingStatus, (error) => (
        <ApiErrorAlert>
          {error.status === 412
            ? behandling.status === IBehandlingStatus.AVBRUTT
              ? 'Behandlingen er avbrutt, vilkårsvurderingen finnes ikke.'
              : 'Virkningstidspunkt og kommer søker tilgode må avklares før vilkårsvurdering kan starte'
            : error.detail}
        </ApiErrorAlert>
      ))}
    </>
  )
}

const AlertWrapper = styled.div`
  margin: 1em 0 2em 4em;
  max-width: 750px;

  button {
    margin-top: 10px;
  }
`
