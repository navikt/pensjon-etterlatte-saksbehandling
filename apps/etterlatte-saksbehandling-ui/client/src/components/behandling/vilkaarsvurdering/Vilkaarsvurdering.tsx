import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, opprettVilkaarsvurdering, Vilkaar } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorder, VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'
import Spinner from '~shared/Spinner'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  updateVilkaarsvurdering,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { Heading } from '@navikt/ds-react'
import { HeadingWrapper, Innhold } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isInitial, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { VilkaarsVurderingKnapper } from '~components/behandling/handlinger/vilkaarsvurderingKnapper'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export const Vilkaarsvurdering = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props

  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const vilkaarsvurdering = behandling.vilkårsprøving
  const behandles = hentBehandlesFraStatus(behandling.status)
  const [vilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
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
          opprettHvisDenIkkeFinnes(behandlingId)
        } else {
          dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
        }
      })
    }
  }, [behandlingId])

  const opprettHvisDenIkkeFinnes = (behandlingId: string) => {
    opprettNyVilkaarsvurdering(behandlingId, (vilkaarsvurdering) => {
      dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
      if (behandling.behandlingType === IBehandlingsType.REVURDERING) {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      }
    })
  }

  const harVilkaarTilVurdering = (vilkaar: Vilkaar[]) => vilkaar.some((v) => v.kopiertFraVilkaar == null)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size={'large'} level={'1'}>
            Vilkårsvurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {behandlingId && vilkaarsvurdering && (
        <>
          <VilkaarBorderTop />
          {vilkaarsvurdering.vilkaar
            .filter((vilkaar) => vilkaar.kopiertFraVilkaar == null)
            .map((vilkaar, index) => (
              <ManueltVilkaar
                key={index}
                vilkaar={vilkaar}
                oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
                behandlingId={behandlingId}
                redigerbar={behandles && !vilkaarsvurdering.resultat && vilkaar.kopiertFraVilkaar == null}
              />
            ))}

          {vilkaarsvurdering.vilkaar.filter((vilkaar) => vilkaar.kopiertFraVilkaar == null).length > 0 ? (
            <Resultat
              virkningstidspunktDato={behandling.virkningstidspunkt?.dato}
              sakstype={behandling.sakType}
              vilkaarsvurdering={vilkaarsvurdering}
              oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
              behandlingId={behandlingId}
              redigerbar={behandles && harVilkaarTilVurdering(vilkaarsvurdering.vilkaar)}
              behandlingstype={behandling.behandlingType}
            />
          ) : (
            <>
              <Innhold>Ingen vilkår behøver vurdering i denne behandlingen</Innhold>
              <VilkaarBorder />
              <BehandlingHandlingKnapper>
                <VilkaarsVurderingKnapper />
              </BehandlingHandlingKnapper>
            </>
          )}
        </>
      )}
      {isPending(vilkaarsvurderingStatus) && <Spinner visible={true} label={'Henter vilkårsvurdering'} />}
      {isPending(opprettNyVilkaarsvurderingStatus) && <Spinner visible={true} label={'Oppretter vilkårsvurdering'} />}
      {isFailure(vilkaarsvurderingStatus) && isInitial(opprettNyVilkaarsvurderingStatus) && (
        <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>
      )}
      {isFailure(opprettNyVilkaarsvurderingStatus) && (
        <ApiErrorAlert>
          {opprettNyVilkaarsvurderingStatus.error.statusCode === 412
            ? 'Virkningstidspunkt og kommer søker tilgode må avklares før vilkårsvurdering kan starte'
            : 'En feil har oppstått'}
        </ApiErrorAlert>
      )}
    </Content>
  )
}
