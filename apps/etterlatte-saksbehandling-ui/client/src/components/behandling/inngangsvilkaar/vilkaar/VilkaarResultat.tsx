import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../../handlinger/vilkaarsvurderingKnapper'
import { NesteOgTilbake } from '../../handlinger/NesteOgTilbake'
import { formaterVedtaksResultat, useVedtaksResultat } from '../../useVedtaksResultat'

type Props = {
  id: any
  dato: string
  behandles: boolean
}

export const VilkaarResultat: React.FC<Props> = ({ id, dato, behandles }) => {
  const vedtaksresultat = useVedtaksResultat()
  const datoFormatert = format(new Date(dato), 'dd.MM.yyyy')
  const tekst = formaterVedtaksResultat(vedtaksresultat, datoFormatert)

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
