import { IVilkaarsvurdering, Vilkaar, VurderingsResultat } from '~shared/api/vilkaarsvurdering'
import { Vurdering } from './Vurdering'
import { StatusIcon, StatusIconProps } from '~shared/icons/statusIcon'
import { Box, Heading, HStack, Link, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
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
      <Box paddingInline="space-16 space-4" paddingBlock="space-4" borderWidth="1 0 0 0">
        <HStack justify="space-between">
          <VStack gap="space-1">
            <HStack align="center" gap="space-4">
              <StatusIcon status={status()} aria-hidden />
              <Heading size="small" level="3">
                {vilkaar.hovedvilkaar.tittel}
              </Heading>
            </HStack>
            {vilkaar.hovedvilkaar.lovreferanse.lenke ? (
              <Link href={vilkaar.hovedvilkaar.lovreferanse.lenke} target="_blank" rel="noopener noreferrer">
                {paragrafType(vilkaar)} {formatertLovreferanse(vilkaar.hovedvilkaar.lovreferanse)}
                <ExternalLinkIcon title={vilkaar.hovedvilkaar.tittel} />
              </Link>
            ) : (
              <>
                {paragrafType(vilkaar)} {vilkaar.hovedvilkaar.lovreferanse.paragraf}
              </>
            )}
            <Box marginBlock="space-2" marginInline="space-0" maxWidth="41rem" style={{ whiteSpace: 'pre-line' }}>
              {vilkaar.hovedvilkaar.beskrivelse}
            </Box>
          </VStack>
          <Vurdering
            vilkaar={vilkaar}
            oppdaterVilkaar={props.oppdaterVilkaar}
            behandlingId={props.behandlingId}
            redigerbar={props.redigerbar}
          />
        </HStack>
      </Box>
    </>
  )
}
