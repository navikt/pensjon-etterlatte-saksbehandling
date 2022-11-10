import { IGyldighetproving, VurderingsResultat } from '~store/reducers/BehandlingReducer'
import { OversiktElement } from '../OversiktElement'

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
  const erOppfylt = innsenderErForelder?.resultat === VurderingsResultat.OPPFYLT

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

  return <OversiktElement navn={navn} label={label} tekst={tekst} erOppfylt={erOppfylt} />
}
