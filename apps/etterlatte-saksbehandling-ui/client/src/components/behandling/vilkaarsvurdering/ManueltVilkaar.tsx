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
import React from 'react'
import { Vilkaar, VurderingsResultat } from '../../../shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import { VurderingsResultat as VurderingsresultatOld } from '../../../store/reducers/BehandlingReducer'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import { VilkaarGrunnlagsStoette } from './vilkaar/VilkaarGrunnlagsStoette'

export interface VilkaarProps {
  vilkaar: Vilkaar
  oppdaterVilkaar: () => void
  behandlingId: string
}

export const ManueltVilkaar = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const status = (): VurderingsresultatOld => {
    if (vilkaar.vurdering) {
      if (vilkaar.vurdering.resultat == VurderingsResultat.OPPFYLT) {
        return VurderingsresultatOld.OPPFYLT
      } else if (vilkaar.vurdering.resultat == VurderingsResultat.IKKE_OPPFYLT) {
        return VurderingsresultatOld.IKKE_OPPFYLT
      }
    }

    return VurderingsresultatOld.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
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
            <VilkaarGrunnlagsStoette vilkaar={vilkaar} />
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <Vurdering
                  vilkaar={vilkaar}
                  oppdaterVilkaar={props.oppdaterVilkaar}
                  behandlingId={props.behandlingId}
                />
              </VilkaarlisteTitle>
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
