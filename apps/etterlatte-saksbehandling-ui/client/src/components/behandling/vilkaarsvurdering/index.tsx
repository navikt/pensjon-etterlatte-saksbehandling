import { Content, Header } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, Vilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'
import { RequestStatus } from './utils'
import Spinner from '~shared/Spinner'
import { format } from 'date-fns'
import { updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'

export const Inngangsvilkaar = () => {
  const location = useLocation()
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<Vilkaarsvurdering | undefined>(undefined)
  const [status, setStatus] = useState<RequestStatus>(RequestStatus.notStarted)

  const oppdaterVilkaarsvurdering = (oppdatertVilkaarsvurdering: Vilkaarsvurdering) => {
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
      <Header>
        <h1>Vilkårsvurdering</h1>
      </Header>

      {behandlingId && status === RequestStatus.ok && vilkaarsvurdering?.virkningstidspunkt && (
        <>
          <VilkaarBorderTop />
          {vilkaarsvurdering.vilkaar.map((value, index) => (
            <ManueltVilkaar
              key={index}
              vilkaar={value}
              oppdaterVilkaar={oppdaterVilkaarsvurdering}
              behandlingId={behandlingId}
            />
          ))}

          <Resultat
            dato={format(Date.parse(vilkaarsvurdering.virkningstidspunkt), 'dd.MM.yyyy')}
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={oppdaterVilkaarsvurdering}
            behandlingId={behandlingId}
          />
        </>
      )}
      {vilkaarsvurdering?.vilkaar.length === 0 && <p>Du har ingen vilkår</p>}
      {status === RequestStatus.isloading && <Spinner visible={true} label={'Henter vilkårsvurdering'} />}
      {status === RequestStatus.error && <p>En feil har oppstått</p>}
    </Content>
  )
}
