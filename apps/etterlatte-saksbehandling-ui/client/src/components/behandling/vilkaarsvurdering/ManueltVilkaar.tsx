import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from '../inngangsvilkaar/styled'
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
              <Title>{vilkaar.paragrafTittel} &nbsp;</Title>
              <a href={vilkaar.paragrafLenke} target="_blank" rel="noopener noreferrer">
                {vilkaar.paragraf}
              </a>
              <br />
              <br />
              <Lovtekst>{vilkaar.lovtekst}</Lovtekst>
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
