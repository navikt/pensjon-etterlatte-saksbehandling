import { IGyldighetproving } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { InfoWrapper } from '../../../styled'
import { Info } from '../../../Info'

interface Props {
  ingenAnnenVergeEnnForelder: IGyldighetproving | undefined
}

export const Verge = ({ ingenAnnenVergeEnnForelder }: Props) => {
  const navn = ingenAnnenVergeEnnForelder?.resultat === VurderingsResultat.OPPFYLT ? '-' : undefined
  const label = 'Verge'
  const tekst = settTekst(ingenAnnenVergeEnnForelder?.resultat)

  function settTekst(vurdering: VurderingsResultat | undefined): string {
    switch (vurdering) {
      case VurderingsResultat.OPPFYLT:
        return ''
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Annen verge enn forelder er registrert'
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
