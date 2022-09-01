import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../../handlinger/vilkaarsvurderingKnapper'
import { NesteOgTilbake } from '../../handlinger/NesteOgTilbake'
import { useVedtaksResultat, VedtakResultat } from '../../useVedtaksResultat'

type Props = {
  id: any
  dato: string
  behandles: boolean
}

const vurderingsresultatTilResultatTekst = (vedtaksresultat: VedtakResultat, dato: string): string => {
  const datoFormatert = format(new Date(dato), 'dd.MM.yyyy')
  switch (vedtaksresultat) {
    case 'innvilget':
      return `Innvilget fra ${datoFormatert}`
    case 'avslag':
      return `Avslag`
    case 'opphoer':
      return `Opphør fra ${datoFormatert}`
    case 'uavklart':
      return `Mangler opplysninger`
    case 'endring':
      return `Endring fra ${datoFormatert}`
  }
}

export const VilkaarResultat: React.FC<Props> = ({ id, dato, behandles }) => {
  const vedtaksresultat = useVedtaksResultat()
  const tekst = vurderingsresultatTilResultatTekst(vedtaksresultat, dato)

  return (
    <>
      <VilkaarBorder id={id}>
        <TekstWrapper>
          Vilkårsresultat: &nbsp; <strong> {tekst}</strong>
        </TekstWrapper>
      </VilkaarBorder>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <VilkaarsVurderingKnapper />
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

const TekstWrapper = styled.div`
  display: flex;
  justify-content: center;
  font-size: 1.2em;
`
