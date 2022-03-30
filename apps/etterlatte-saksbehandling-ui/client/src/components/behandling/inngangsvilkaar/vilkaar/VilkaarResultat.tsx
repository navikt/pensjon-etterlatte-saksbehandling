import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'

type Props = {
  id: any
  resultat: VilkaarVurderingsResultat | undefined
}

export const VilkaarResultat: React.FC<Props> = ({ id, resultat }) => {
  let tekst
  if (resultat === VilkaarVurderingsResultat.OPPFYLT) {
    tekst = 'Innvilget fra DD.MM.ÅÅÅÅ'
  } else if (resultat === VilkaarVurderingsResultat.IKKE_OPPFYLT) {
    tekst = 'Avslag'
  } else {
    tekst = 'Trenger avklaring'
  }
  return (
    <VilkaarBorder id={id}>
      <TekstWrapper>
        Vilkårsresultat: &nbsp; <strong> {tekst}</strong>
      </TekstWrapper>
    </VilkaarBorder>
  )
}

const TekstWrapper = styled.div`
  display: flex;
  justify-content: center;
  font-size: 1.2em;
`
