import {
  Innhold,
  Title,
  VilkaarBeskrivelse,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from './styled'
import { IVilkaarsvurdering, Vilkaar, VurderingsResultat } from '~shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import { StatusIcon, StatusIconProps } from '~shared/icons/statusIcon'
import { Link } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { formatertLovreferanse } from '~components/behandling/vilkaarsvurdering/utils'

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

  const paragrafType = (vilkaar: Vilkaar) => {
    if (vilkaar.hovedvilkaar.lovreferanse.paragraf.startsWith('§')) {
      return 'Folketrygden'
    }

    return ''
  }

  return (
    <>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>
                <StatusIcon status={status()} />
                {vilkaar.hovedvilkaar.tittel}
              </Title>
              {vilkaar.hovedvilkaar.lovreferanse.lenke ? (
                <Link href={vilkaar.hovedvilkaar.lovreferanse.lenke} target="_blank" rel="noopener noreferrer">
                  {`${paragrafType(vilkaar)} ${formatertLovreferanse(vilkaar.hovedvilkaar.lovreferanse)}`}
                  <ExternalLinkIcon title={vilkaar.hovedvilkaar.tittel} />
                </Link>
              ) : (
                <>{`${paragrafType(vilkaar)} ${vilkaar.hovedvilkaar.lovreferanse.paragraf}`}</>
              )}
              <VilkaarBeskrivelse>{vilkaar.hovedvilkaar.beskrivelse}</VilkaarBeskrivelse>
            </VilkaarColumn>
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

      <Border />
    </>
  )
}
