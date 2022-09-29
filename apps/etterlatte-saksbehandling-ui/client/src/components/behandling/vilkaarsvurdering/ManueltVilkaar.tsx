import {
  Innhold,
  Title,
  Lovtekst,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from './styled'
import React, { ReactNode } from 'react'
import { Vilkaar } from '../../../shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'

export interface VilkaarProps {
  vilkaar: Vilkaar
  oppdaterVilkaar: () => void
  children: ReactNode
}

export const ManueltVilkaar = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  return (
    <VilkaarBorder id={vilkaar.type.toString()}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>{vilkaar.paragraf.tittel} &nbsp;</Title>
              <a href={vilkaar.paragraf.lenke} target="_blank" rel="noopener noreferrer">
                {vilkaar.paragraf.paragraf}
              </a>
              <Lovtekst>{vilkaar.paragraf.lovtekst}</Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>{props.children}</VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <Vurdering vilkaar={vilkaar} oppdaterVilkaar={props.oppdaterVilkaar} />
              </VilkaarlisteTitle>
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
