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
} from './styled'
import React, { ReactNode } from 'react'
import { Vilkaar } from '../../../shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import {
  VurderingsResultat,
  VurderingsResultat as VurderingsresultatOld,
} from '../../../store/reducers/BehandlingReducer'
import { StatusIcon } from '../../../shared/icons/statusIcon'

export interface VilkaarProps {
  vilkaar: Vilkaar
  oppdaterVilkaar: () => void
  children: ReactNode
}

export const ManueltVilkaar = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  const erVurdert = !!vilkaar.vurdering
  const erOppfyllt = vilkaar.vurdering?.resultat == VurderingsResultat.OPPFYLT

  const status = (): VurderingsresultatOld => {
    if (erVurdert) {
      return erOppfyllt ? VurderingsresultatOld.OPPFYLT : VurderingsresultatOld.IKKE_OPPFYLT
    } else {
      return VurderingsresultatOld.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }
  }

  return (
    <VilkaarBorder id={vilkaar.type.toString()}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>
                <StatusIcon status={status()} noLeftPadding />
                {vilkaar.paragraf.tittel}
              </Title>

              <Lovtekst>
                <p>
                  <a href={vilkaar.paragraf.lenke} target="_blank" rel="noopener noreferrer">
                    {vilkaar.paragraf.paragraf}
                  </a>
                  : {vilkaar.paragraf.lovtekst}
                </p>
              </Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>{props.children}</VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <Vurdering
                  vilkaar={vilkaar}
                  oppdaterVilkaar={props.oppdaterVilkaar}
                  erOppfylt={erOppfyllt}
                  erVurdert={erVurdert}
                />
              </VilkaarlisteTitle>
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
