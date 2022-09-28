import { Content, Header } from '../../../shared/styled'
import React, { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, Vilkaarsvurdering } from '../../../shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarResultat } from '../inngangsvilkaar/vilkaar/VilkaarResultat'
import { VilkaarBorderTop } from './styled'

export const Inngangsvilkaar = () => {
  const location = useLocation()
  const { behandlingId } = useParams()

  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<Vilkaarsvurdering>({ vilkaar: [] })

  const hentVilkaarsvurderingForBehandling = () => {
    hentVilkaarsvurdering(behandlingId!!).then((response) => {
      if (response.status == 'ok') {
        setVilkaarsvurdering(response.data)
      }
    })
  }

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  useEffect(() => {
    hentVilkaarsvurderingForBehandling()
  }, [behandlingId])

  return (
    <Content>
      <Header>
        <h1>Vilkårsvurdering</h1>
      </Header>
      <VilkaarBorderTop />
      {vilkaarsvurdering.vilkaar.map((value, index) => (
        <ManueltVilkaar key={index} vilkaar={value} oppdaterVilkaar={hentVilkaarsvurderingForBehandling}>
          <></>
        </ManueltVilkaar>
      ))}

      {/* todo: resultat skal ikke lengre hentes fra behandlingscontext */}
      <VilkaarResultat id="vilkaarResultat" dato={'2020-01-01'} behandles={false} />
    </Content>
  )
}
