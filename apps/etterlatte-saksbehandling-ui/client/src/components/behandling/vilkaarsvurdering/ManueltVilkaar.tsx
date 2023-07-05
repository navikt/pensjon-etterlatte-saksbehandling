import {
  Innhold,
  VilkaarBeskrivelse,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from './styled'
import { Vilkaar, IVilkaarsvurdering, VurderingsResultat } from '~shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import { StatusIcon, StatusIconProps } from '~shared/icons/statusIcon'
import { VilkaarGrunnlagsStoette } from './vilkaar/VilkaarGrunnlagsStoette'
import { Link } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'

export interface VilkaarProps {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
  redigerbar: boolean
}

export const ManueltVilkaar = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const status = (): StatusIconProps => {
    if (vilkaar.vurdering) {
      if (
        vilkaar.hovedvilkaar.resultat == VurderingsResultat.OPPFYLT ||
        vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
      ) {
        return 'success'
      } else if (
        vilkaar.hovedvilkaar.resultat == VurderingsResultat.IKKE_OPPFYLT &&
        !vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
      ) {
        return 'error'
      }
    }

    return 'warning'
  }

  return (
    <VilkaarBorder id={vilkaar.hovedvilkaar.type}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>
                <StatusIcon status={status()} />
                {vilkaar.hovedvilkaar.tittel}
              </Title>
              <Link href={vilkaar.hovedvilkaar.lovreferanse.lenke} target="_blank" rel="noopener noreferrer">
                {`Folketrygden ${vilkaar.hovedvilkaar.lovreferanse.paragraf}`}
                <ExternalLinkIcon title={vilkaar.hovedvilkaar.tittel} />
              </Link>
              <VilkaarBeskrivelse>{vilkaar.hovedvilkaar.beskrivelse}</VilkaarBeskrivelse>
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
