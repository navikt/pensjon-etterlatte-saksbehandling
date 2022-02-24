import { IKriterie, VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { mapKriterieTilSvar, mapKriterieTypeTilTekst } from './utils'

export const VilkaarVurderingsliste = ({ kriterie }: { kriterie: IKriterie[] }) => {
  const oppfylt = kriterie.filter((krit) => krit.resultat === VilkaarVurderingsResultat.OPPFYLT)
  const ikkeOppfylt = kriterie.filter((krit) => krit.resultat === VilkaarVurderingsResultat.IKKE_OPPFYLT)
  const mangler = kriterie.filter(
    (krit) => krit.resultat === VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
  )
  const liste = [oppfylt, ikkeOppfylt, mangler].flat()

  return (
    <div>
      {liste.map((krit) => (
        <div key={krit.navn} style={{ display: 'flex', marginTop: '10px' }}>
          <div style={{ marginRight: '20px', marginTop: '10px' }}>
            <StatusIcon status={krit.resultat} />
          </div>

          <div>
            <div style={{ fontWeight: 'bold' }}>{mapKriterieTypeTilTekst(krit.navn)}</div>
            <div>{mapKriterieTilSvar(krit)}</div>
          </div>
        </div>
      ))}
    </div>
  )
}
