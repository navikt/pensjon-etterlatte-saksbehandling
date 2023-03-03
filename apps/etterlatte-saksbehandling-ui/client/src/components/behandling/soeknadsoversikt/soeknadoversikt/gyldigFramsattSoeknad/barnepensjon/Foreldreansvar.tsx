import { IGyldighetproving } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { InfoWrapper } from '../../../styled'
import { Info } from '../../../Info'

interface Props {
  innsenderHarForeldreansvar: IGyldighetproving | undefined
}

export const Foreldreansvar = ({ innsenderHarForeldreansvar }: Props) => {
  const navn =
    innsenderHarForeldreansvar?.resultat === VurderingsResultat.OPPFYLT
      ? innsenderHarForeldreansvar?.basertPaaOpplysninger?.innsender?.navn
      : undefined
  const label = 'Foreldreansvar'
  const tekst = settTekst(innsenderHarForeldreansvar?.resultat)

  function settTekst(vurdering: VurderingsResultat | undefined): string {
    switch (vurdering) {
      case VurderingsResultat.OPPFYLT:
        return ''
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Innsender har ikke foreldreansvar'
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
