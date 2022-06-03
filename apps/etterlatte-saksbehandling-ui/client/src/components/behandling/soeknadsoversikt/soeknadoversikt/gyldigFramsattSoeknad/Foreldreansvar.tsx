import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'
import { OversiktElement } from '../OversiktElement'

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
  const erOppfylt = innsenderHarForeldreansvar?.resultat === VurderingsResultat.OPPFYLT

  function settTekst(vurdering: VurderingsResultat | undefined): string {
    switch (vurdering) {
      case VurderingsResultat.OPPFYLT:
        return '(gjenlevende forelder)'
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Innsender har ikke foreldreansvar'
      default:
        return 'Mangler info'
    }
  }

  return <OversiktElement navn={navn} label={label} tekst={tekst} erOppfylt={erOppfylt} />
}
