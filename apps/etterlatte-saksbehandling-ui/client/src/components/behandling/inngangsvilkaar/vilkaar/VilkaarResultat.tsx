import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../../handlinger/vilkaarsvurderingKnapper'

type Props = {
  id: any
  resultat: VurderingsResultat | undefined
  dato: string
}

export const VilkaarResultat: React.FC<Props> = ({ id, resultat, dato }) => {
  const datoFormatert = format(new Date(dato), 'dd.MM.yyyy')

  let tekst = ''
  if (resultat === VurderingsResultat.OPPFYLT) {
    tekst = 'Innvilget fra ' + datoFormatert
  } else if (resultat === VurderingsResultat.IKKE_OPPFYLT) {
    tekst = 'Avslag fra ' + datoFormatert
  } else {
    tekst = 'Trenger avklaring'
  }

  return (
    <>
      <VilkaarBorder id={id}>
        <TekstWrapper>
          Vilkårsresultat: &nbsp; <strong> {tekst}</strong>
        </TekstWrapper>
      </VilkaarBorder>

      <BehandlingHandlingKnapper>
        <VilkaarsVurderingKnapper vilkaarResultat={resultat} />
      </BehandlingHandlingKnapper>
    </>
  )
}

const TekstWrapper = styled.div`
  display: flex;
  justify-content: center;
  font-size: 1.2em;
`
