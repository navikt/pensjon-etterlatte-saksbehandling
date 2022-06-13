import { IVilkaarResultat, VilkaarsType, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentKommerBarnetTilgodeVurderingsTekst } from '../../utils'
import { VurderingsTitle, Undertekst, VurderingsContainer } from '../../styled'

export const KommerBarnetTilGodeVurdering = ({
  kommerSoekerTilgodeVurdering,
}: {
  kommerSoekerTilgodeVurdering: IVilkaarResultat
}) => {
  const hentTekst = (): any => {
    const sammeAdresse = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE
    )
    const barnIngenUtland = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.BARN_INGEN_OPPGITT_UTLANDSADRESSE
    )
    const sammeAdresseAvdoed = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.BARN_BOR_PAA_AVDOEDES_ADRESSE
    )

    return hentKommerBarnetTilgodeVurderingsTekst(
      sammeAdresse?.resultat,
      barnIngenUtland?.resultat,
      sammeAdresseAvdoed?.resultat
    )
  }

  const tittel =
    kommerSoekerTilgodeVurdering.resultat !== VurderingsResultat.OPPFYLT
      ? 'Ikke sannsynlig pensjon kommer barnet til gode'
      : 'Sannsynlig pensjonen kommer barnet til gode'

  return (
    <VurderingsContainer>
      <div>
        {kommerSoekerTilgodeVurdering.resultat && (
          <GyldighetIcon status={kommerSoekerTilgodeVurdering.resultat} large={true} />
        )}
      </div>
      <div>
        <VurderingsTitle>{tittel}</VurderingsTitle>
        <Undertekst gray={true}>
          Automatisk {format(new Date(kommerSoekerTilgodeVurdering.vurdertDato), 'dd.MM.yyyy')}
        </Undertekst>
        {kommerSoekerTilgodeVurdering?.resultat !== VurderingsResultat.OPPFYLT && (
          <Undertekst gray={false}>{hentTekst()}</Undertekst>
        )}
      </div>
    </VurderingsContainer>
  )
}
