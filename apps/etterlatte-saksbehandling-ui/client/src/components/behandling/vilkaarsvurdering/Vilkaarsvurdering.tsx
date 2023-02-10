import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, opprettVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'
import Spinner from '~shared/Spinner'
import { updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isInitial, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Vilkaarsvurdering = () => {
  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
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
    opprettNyVilkaarsvurdering(behandlingId, (vilkaarsvurdering) =>
      dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
    )
  }

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
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
            behandlingId={behandlingId}
            redigerbar={behandles}
          />
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
