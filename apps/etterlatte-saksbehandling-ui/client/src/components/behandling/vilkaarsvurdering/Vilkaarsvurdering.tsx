import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'
import Spinner from '~shared/Spinner'
import { updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'

export const Vilkaarsvurdering = () => {
  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const behandlingstatus = useAppSelector((state) => state.behandlingReducer.behandling.status)
  const behandles = hentBehandlesFraStatus(behandlingstatus)
  const [vilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    fetchVilkaarsvurdering(behandlingId, (vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering)))
  }, [behandlingId])

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size={'large'} level={'1'}>
            Vilkårsvurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {behandlingId && isSuccess(vilkaarsvurderingStatus) && vilkaarsvurderingStatus.data.virkningstidspunkt && (
        <>
          <VilkaarBorderTop />
          {vilkaarsvurderingStatus.data.vilkaar.map((value, index) => (
            <ManueltVilkaar
              key={index}
              vilkaar={value}
              oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
              behandlingId={behandlingId}
              redigerbar={behandles && !vilkaarsvurderingStatus.data?.resultat}
            />
          ))}
          {vilkaarsvurderingStatus.data.vilkaar.length === 0 && <p>Du har ingen vilkår</p>}

          <Resultat
            virkningstidspunktDato={vilkaarsvurderingStatus.data.virkningstidspunkt}
            vilkaarsvurdering={vilkaarsvurderingStatus.data}
            oppdaterVilkaar={(vilkaarsvurdering) => dispatch(updateVilkaarsvurdering(vilkaarsvurdering))}
            behandlingId={behandlingId}
            redigerbar={behandles}
          />
        </>
      )}
      {isPending(vilkaarsvurderingStatus) && <Spinner visible={true} label={'Henter vilkårsvurdering'} />}
      {isFailure(vilkaarsvurderingStatus) &&
        (vilkaarsvurderingStatus.error.statusCode === 412 ? (
          <p>Virkningstidspunkt må avklares før vilkårsvurdering kan starte</p>
        ) : (
          <p>En feil har oppstått</p>
        ))}
    </Content>
  )
}
