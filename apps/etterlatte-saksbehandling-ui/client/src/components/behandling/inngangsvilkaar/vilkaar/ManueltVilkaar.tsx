import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
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
} from '../styled'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { vilkaarErOppfylt } from './tekstUtils'
import React, { ReactNode } from 'react'
import { Vilkaar } from '../../../../shared/api/vilkaarsvurdering'

export interface VilkaarProps {
  vilkaar: Vilkaar
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
              <Title>{vilkaar.paragraf}</Title>
              <Lovtekst>{vilkaar.lovtekst}</Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>{props.children}</VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <StatusIcon status={VurderingsResultat.OPPFYLT} large /> {vilkaarErOppfylt(VurderingsResultat.OPPFYLT)}
              </VilkaarlisteTitle>
              {/*<KildeDatoVilkaar isHelautomatisk={false} dato={Date.now().toString()} />*/}
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
