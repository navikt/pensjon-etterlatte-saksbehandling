import { Content, Header } from '../../../shared/styled'
import React, { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { VilkaarBorderTop } from '../inngangsvilkaar/styled'
import { hentVilkaarsvurdering, Vilkaarsvurdering } from '../../../shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarResultat } from '../inngangsvilkaar/vilkaar/VilkaarResultat'

export const Inngangsvilkaar = () => {
  const location = useLocation()
  const { behandlingId } = useParams()

  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<Vilkaarsvurdering>({ vilkaar: [] })

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  useEffect(() => {
    hentVilkaarsvurdering(behandlingId!!).then((response) => {
      if (response.status == 'ok') {
        setVilkaarsvurdering(response.data)
      }
    })
  })

  return (
    <Content>
      <Header>
        <h1>Vilkårsvurdering</h1>
      </Header>
      <VilkaarBorderTop />
      {vilkaarsvurdering.vilkaar.map((value, index) => (
        <ManueltVilkaar key={index} vilkaar={value}>
          <p>Her kommer det (kanskje) støtte til vilkåret</p>
        </ManueltVilkaar>
      ))}

      {/* todo: resultat skal ikke lengre hentes fra behandlingscontext */}
      <VilkaarResultat id="vilkaarResultat" dato={'2020-01-01'} behandles={false} />
    </Content>
  )
}
