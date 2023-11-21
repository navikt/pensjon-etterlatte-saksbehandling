import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
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
import { Alert, BodyLong, Button, Heading } from '@navikt/ds-react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { isFailure, isInitial, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import {
  behandlingGjelderBarnepensjonPaaNyttRegelverk,
  vilkaarsvurderingErPaaNyttRegelverk,
} from '~components/behandling/vilkaarsvurdering/utils'

export const Vilkaarsvurdering = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props

  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const vilkaarsvurdering = behandling.vilkårsprøving
  const behandles = behandlingErRedigerbar(behandling.status)
  const [vilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [slettVilkaarsvurderingStatus, slettGammelVilkaarsvurdering] = useApiCall(slettVilkaarsvurdering)
  const [opprettNyVilkaarsvurderingStatus, opprettNyVilkaarsvurdering] = useApiCall(opprettVilkaarsvurdering)

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

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
    behandles &&
    !vilkaarsvurderingErPaaNyttRegelverk(vilkaarsvurdering) &&
    behandlingGjelderBarnepensjonPaaNyttRegelverk(behandling)

  const resetVilkaarsvurdering = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    slettGammelVilkaarsvurdering(behandlingId, () => {
      dispatch(updateVilkaarsvurdering(undefined))
      createVilkaarsvurdering(behandlingId, false)
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size="large" level="1">
            Vilkårsvurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

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
              {isFailure(slettVilkaarsvurderingStatus) && (
                <ApiErrorAlert>Klarte ikke slette vilkårsvurderingen</ApiErrorAlert>
              )}
            </AlertWrapper>
          )}

          <Border />

          {vilkaarsvurdering.vilkaar.map((value, index) => (
            <ManueltVilkaar
              key={index}
              vilkaar={value}
              oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
              behandlingId={behandlingId}
              redigerbar={behandles && !vilkaarsvurdering.resultat}
            />
          ))}
          {vilkaarsvurdering.vilkaar.length === 0 && <p>Du har ingen vilkår</p>}

          <Resultat
            virkningstidspunktDato={behandling.virkningstidspunkt?.dato}
            sakstype={behandling.sakType}
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
            behandlingId={behandlingId}
            redigerbar={behandles}
            behandlingstype={behandling.behandlingType}
          />
        </>
      )}
      {isPending(vilkaarsvurderingStatus) && <Spinner visible={true} label="Henter vilkårsvurdering" />}
      {isPending(opprettNyVilkaarsvurderingStatus) && <Spinner visible={true} label="Oppretter vilkårsvurdering" />}
      {isPending(slettVilkaarsvurderingStatus) && <Spinner visible={true} label="Sletter vilkårsvurdering" />}
      {isFailure(vilkaarsvurderingStatus) && isInitial(opprettNyVilkaarsvurderingStatus) && (
        <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
      )}
      {isFailure(opprettNyVilkaarsvurderingStatus) && (
        <ApiErrorAlert>
          {opprettNyVilkaarsvurderingStatus.error.status === 412
            ? 'Virkningstidspunkt og kommer søker tilgode må avklares før vilkårsvurdering kan starte'
            : 'En feil har oppstått'}
        </ApiErrorAlert>
      )}
    </Content>
  )
}

const AlertWrapper = styled.div`
  margin: 1em 0 2em 4em;
  max-width: 750px;

  button {
    margin-top: 10px;
  }
`
