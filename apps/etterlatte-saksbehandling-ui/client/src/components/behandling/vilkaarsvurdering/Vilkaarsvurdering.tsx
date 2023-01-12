import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'
import { RequestStatus } from './utils'
import Spinner from '~shared/Spinner'
import { updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { Heading } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'

export const Vilkaarsvurdering = () => {
  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<IVilkaarsvurdering | undefined>(undefined)
  const [status, setStatus] = useState<RequestStatus>(RequestStatus.notStarted)
  const behandlingstatus = useAppSelector((state) => state.behandlingReducer.behandling.status)
  const behandles = hentBehandlesFraStatus(behandlingstatus)

  const oppdaterVilkaarsvurdering = (oppdatertVilkaarsvurdering: IVilkaarsvurdering) => {
    setVilkaarsvurdering(oppdatertVilkaarsvurdering)
    if (oppdatertVilkaarsvurdering.resultat) {
      dispatch(updateVilkaarsvurdering(oppdatertVilkaarsvurdering))
    }
  }

  const hentVilkaarsvurderingLocal = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    setStatus(RequestStatus.isloading)
    hentVilkaarsvurdering(behandlingId)
      .then((response) => {
        if (response.status == 'ok') {
          setStatus(RequestStatus.ok)
          oppdaterVilkaarsvurdering(response.data)
        } else if (response.statusCode == 412) {
          setStatus(RequestStatus.preconditionFailed)
        } else {
          setStatus(RequestStatus.error)
        }
      })
      .catch(() => {
        setStatus(RequestStatus.error)
      })
  }

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  useEffect(() => {
    hentVilkaarsvurderingLocal()
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

      {behandlingId && status === RequestStatus.ok && vilkaarsvurdering?.virkningstidspunkt && (
        <>
          <VilkaarBorderTop />
          {vilkaarsvurdering.vilkaar.map((value, index) => (
            <ManueltVilkaar
              key={index}
              vilkaar={value}
              oppdaterVilkaar={oppdaterVilkaarsvurdering}
              behandlingId={behandlingId}
              kunLesetilgang={!behandles}
            />
          ))}

          <Resultat
            virkningstidspunktDato={vilkaarsvurdering.virkningstidspunkt}
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={oppdaterVilkaarsvurdering}
            behandlingId={behandlingId}
            kunLesetilgang={!behandles}
          />
        </>
      )}
      {vilkaarsvurdering?.vilkaar.length === 0 && <p>Du har ingen vilkår</p>}
      {status === RequestStatus.isloading && <Spinner visible={true} label={'Henter vilkårsvurdering'} />}
      {status === RequestStatus.error && <p>En feil har oppstått</p>}
      {status === RequestStatus.preconditionFailed && (
        <p>Virkningstidspunkt må avklares før vilkårsvurdering kan starte</p>
      )}
    </Content>
  )
}
