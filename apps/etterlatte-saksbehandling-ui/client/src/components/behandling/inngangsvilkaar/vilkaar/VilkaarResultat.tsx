import { IBehandlingStatus, VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import React from 'react'
import { VilkaarBorder } from '../styled'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../../handlinger/vilkaarsvurderingKnapper'
import { NesteOgTilbake } from '../../handlinger/NesteOgTilbake'

type Props = {
  id: any
  resultat: VurderingsResultat | undefined
  dato: string
  behandlingStatus: IBehandlingStatus
}

export const VilkaarResultat: React.FC<Props> = ({ id, resultat, dato, behandlingStatus }) => {
  const datoFormatert = format(new Date(dato), 'dd.MM.yyyy')

  let tekst = ''
  if (resultat === VurderingsResultat.OPPFYLT) {
    tekst = 'Innvilget fra ' + datoFormatert
  } else if (resultat === VurderingsResultat.IKKE_OPPFYLT) {
    tekst = 'Avslag fra ' + datoFormatert
  } else {
    tekst = 'Trenger avklaring'
  }

  const behandles =
    behandlingStatus === IBehandlingStatus.UNDER_BEHANDLING || behandlingStatus === IBehandlingStatus.GYLDIG_SOEKNAD

  return (
    <>
      <VilkaarBorder id={id}>
        <TekstWrapper>
          Vilkårsresultat: &nbsp; <strong> {tekst}</strong>
        </TekstWrapper>
      </VilkaarBorder>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <VilkaarsVurderingKnapper vilkaarResultat={resultat} />
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
