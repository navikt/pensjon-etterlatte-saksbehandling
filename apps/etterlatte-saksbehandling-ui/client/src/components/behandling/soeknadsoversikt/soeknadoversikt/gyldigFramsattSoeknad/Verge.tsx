import { IGyldighetproving, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { OversiktElement } from '../OversiktElement'

interface Props {
  ingenAnnenVergeEnnForelder: IGyldighetproving | undefined
}

export const Verge = ({ ingenAnnenVergeEnnForelder }: Props) => {
  const navn = ingenAnnenVergeEnnForelder?.resultat === VurderingsResultat.OPPFYLT ? '-' : undefined
  const label = 'Verge'
  const tekst = settTekst(ingenAnnenVergeEnnForelder?.resultat)
  const erOppfylt = ingenAnnenVergeEnnForelder?.resultat === VurderingsResultat.OPPFYLT

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

  return <OversiktElement navn={navn} label={label} tekst={tekst} erOppfylt={erOppfylt} />
}
