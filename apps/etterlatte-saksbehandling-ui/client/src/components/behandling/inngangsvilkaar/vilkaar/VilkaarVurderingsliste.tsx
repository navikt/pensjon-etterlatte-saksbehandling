import { IKriterie, VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { mapKriterietyperTilTekst } from './tekstUtils'

export const VilkaarVurderingsliste = ({ kriterie }: { kriterie: IKriterie[] }) => {
  const oppfylt = kriterie.filter((krit) => krit.resultat === VurderingsResultat.OPPFYLT)
  const ikkeOppfylt = kriterie.filter((krit) => krit.resultat === VurderingsResultat.IKKE_OPPFYLT)
  const mangler = kriterie.filter(
    (krit) => krit.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
  )
  const liste = [oppfylt, ikkeOppfylt, mangler].flat()

  return (
    <div>
      {liste.map((krit) => (
        <VilkaarVurderingEnkeltElement
          key={krit.navn}
          tittel={mapKriterietyperTilTekst(krit).tittel}
          svar={mapKriterietyperTilTekst(krit).svar}
        />
      ))}
    </div>
  )
}

export const VilkaarVurderingEnkeltElement = ({ tittel, svar }: { tittel: string; svar: string }) => {
  return (
    <div style={{ display: 'flex', marginTop: '10px', paddingLeft: '55px' }}>
      <div>
        <div style={{ fontWeight: 'bold' }}>{tittel}</div>
        <div>{svar}</div>
      </div>
    </div>
  )
}
