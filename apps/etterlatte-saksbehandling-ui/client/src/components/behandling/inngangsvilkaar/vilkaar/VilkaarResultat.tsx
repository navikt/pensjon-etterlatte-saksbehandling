import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import moment from 'moment'

type Props = {
  id: any
  resultat: VilkaarVurderingsResultat | undefined
  dato: string
}

/** Bare funksjoner for at Arnt skulle få teste grensesnittet */
const randomizeForTest = (): VilkaarVurderingsResultat => {
  const result = [VilkaarVurderingsResultat.OPPFYLT, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, VilkaarVurderingsResultat.IKKE_OPPFYLT]
  return result[Math.floor(Math.random() * 3)]
}
const resultat = randomizeForTest() //TODO: fjern
/** Bare funksjoner for at Arnt skulle få teste grensesnittet */

export const VilkaarResultat: React.FC<Props> = ({ id, /*resultat,*/ dato }) => {
  const datoFormatert = moment(dato).format('DD.MM.YYYY')
  
  let tekst = "";
  if (resultat === VilkaarVurderingsResultat.OPPFYLT) {
    tekst = 'Innvilget fra ' + datoFormatert
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
