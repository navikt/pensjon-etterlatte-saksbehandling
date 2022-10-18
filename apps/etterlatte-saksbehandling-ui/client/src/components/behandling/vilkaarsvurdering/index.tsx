import { Content, Header } from '../../../shared/styled'
import React, { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { hentVilkaarsvurdering, Vilkaarsvurdering } from '../../../shared/api/vilkaarsvurdering'
import { ManueltVilkaar } from './ManueltVilkaar'
import { VilkaarBorderTop } from './styled'
import { Resultat } from './Resultat'

export const Inngangsvilkaar = () => {
  const location = useLocation()
  const { behandlingId } = useParams()
  const virk = Date() // todo: Hente korrekt virkningsdato. Se EY-946.
  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<Vilkaarsvurdering>({ vilkaar: [] })

  const oppdaterVilkaarsvurdering = (oppdatertVilkaarsvurdering?: Vilkaarsvurdering) => {
    if (oppdatertVilkaarsvurdering) {
      setVilkaarsvurdering(oppdatertVilkaarsvurdering)
    } else {
      if (!behandlingId) throw new Error('Mangler behandlingsid')
      hentVilkaarsvurdering(behandlingId).then((response) => {
        if (response.status == 'ok') {
          setVilkaarsvurdering(response.data)
        }
      })
    }
  }

  useEffect(() => {
    const hash = location.hash.slice(1)
    document.getElementById(hash)?.scrollIntoView({ behavior: 'smooth', block: 'end', inline: 'nearest' })
  }, [location.hash])

  useEffect(() => {
    oppdaterVilkaarsvurdering()
  }, [behandlingId])

  return (
    <Content>
      <Header>
        <h1>Vilkårsvurdering</h1>
      </Header>

      {behandlingId && (
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
            dato={virk}
            vilkaarsvurdering={vilkaarsvurdering}
            oppdaterVilkaar={oppdaterVilkaarsvurdering}
            behandlingId={behandlingId}
          />
        </>
      )}
    </Content>
  )
}
