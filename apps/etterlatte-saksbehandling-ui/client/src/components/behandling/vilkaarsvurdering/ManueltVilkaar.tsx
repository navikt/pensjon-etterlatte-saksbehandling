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
import { Vilkaar, IVilkaarsvurdering, VurderingsResultat } from '~shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import { VurderingsResultat as VurderingsresultatOld } from '~shared/types/VurderingsResultat'
import { StatusIcon } from '~shared/icons/statusIcon'
import { VilkaarGrunnlagsStoette } from './vilkaar/VilkaarGrunnlagsStoette'
import { Link } from '@navikt/ds-react'

export interface VilkaarProps {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
  redigerbar: boolean
}

export const ManueltVilkaar = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const status = (): VurderingsresultatOld => {
    if (vilkaar.vurdering) {
      if (
        vilkaar.hovedvilkaar.resultat == VurderingsResultat.OPPFYLT ||
        vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
      ) {
        return VurderingsresultatOld.OPPFYLT
      } else if (
        vilkaar.hovedvilkaar.resultat == VurderingsResultat.IKKE_OPPFYLT &&
        !vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
      ) {
        return VurderingsresultatOld.IKKE_OPPFYLT
      }
    }

    return VurderingsresultatOld.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
  }

  return (
    <VilkaarBorder id={vilkaar.hovedvilkaar.type}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>
                <StatusIcon status={status()} noLeftPadding />
                <Link href={vilkaar.hovedvilkaar.lovreferanse.lenke} target="_blank" rel="noopener noreferrer">
                  {vilkaar.hovedvilkaar.lovreferanse.paragraf} {vilkaar.hovedvilkaar.tittel}
                </Link>
              </Title>
              <Lovtekst>
                <p>{vilkaar.hovedvilkaar.beskrivelse}</p>
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
                  redigerbar={props.redigerbar}
                />
              </VilkaarlisteTitle>
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
