import { VurderingsResultat } from '~store/reducers/BehandlingReducer'
import { OversiktElement } from '../OversiktElement'

export const Adresse = ({ resultat, tekst }: { resultat: VurderingsResultat | undefined; tekst: string }) => {
  const navn = undefined
  const label = 'Adresse'
  const erOppfylt = resultat === VurderingsResultat.OPPFYLT

  return <OversiktElement navn={navn} label={label} tekst={tekst} erOppfylt={erOppfylt} />
}
