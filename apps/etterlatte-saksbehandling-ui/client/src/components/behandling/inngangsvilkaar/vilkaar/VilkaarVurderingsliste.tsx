import { IKriterie, VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { mapKriterietyperTilTekst } from './utils'

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
          resultat={krit.resultat}
        />
      ))}
    </div>
  )
}

export const VilkaarVurderingEnkeltElement = ({
  tittel,
  svar,
  resultat,
}: {
  tittel: String
  svar: String
  resultat: VurderingsResultat
}) => {
  return (
    <div style={{ display: 'flex', marginTop: '10px' }}>
      <div style={{ marginTop: '10px' }}>
        <StatusIcon status={resultat} />
      </div>

      <div>
        <div style={{ fontWeight: 'bold' }}>{tittel}</div>
        <div>{svar}</div>
      </div>
    </div>
  )
}
