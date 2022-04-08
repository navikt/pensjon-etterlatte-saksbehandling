import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import moment from 'moment'

type Props = {
  id: any
  resultat: VurderingsResultat | undefined
  dato: string
}

export const VilkaarResultat: React.FC<Props> = ({ id, resultat, dato }) => {
  const datoFormatert = moment(dato).format('DD.MM.YYYY')

  let tekst = ''
  if (resultat === VurderingsResultat.OPPFYLT) {
    tekst = 'Innvilget fra ' + datoFormatert
  } else if (resultat === VurderingsResultat.IKKE_OPPFYLT) {
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
