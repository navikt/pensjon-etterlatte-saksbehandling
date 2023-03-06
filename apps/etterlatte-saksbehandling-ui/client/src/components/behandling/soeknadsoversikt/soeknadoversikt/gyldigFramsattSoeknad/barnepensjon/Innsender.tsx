import { IGyldighetproving } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { InfoWrapper } from '../../../styled'
import { Info } from '../../../Info'

interface Props {
  innsenderErForelder: IGyldighetproving | undefined
}

export const Innsender = ({ innsenderErForelder }: Props) => {
  const navn =
    innsenderErForelder?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
      ? undefined
      : innsenderErForelder?.basertPaaOpplysninger?.innsender?.navn
  const label = 'Innsender'
  const tekst = settTekst(innsenderErForelder?.resultat)

  function settTekst(vurdering: VurderingsResultat | undefined): string {
    switch (vurdering) {
      case VurderingsResultat.OPPFYLT:
        return '(gjenlevende forelder)'
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Ikke gjenlevende forelder'
      default:
        return 'Mangler info'
    }
  }

  return (
    <InfoWrapper>
      <Info tekst={navn ?? 'Ukjent'} undertekst={tekst} label={label} />
    </InfoWrapper>
  )
}
