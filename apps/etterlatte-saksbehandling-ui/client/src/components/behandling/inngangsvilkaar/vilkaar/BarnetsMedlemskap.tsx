import { VilkaarProps } from '../types'
import {
  Innhold,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
} from '../styled'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'

export const BarnetsMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  /*  const bostedadresse = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.soeker_bostedadresse
    )*/

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon status={props.vilkaar.resultat} large={true} /> Barnets medlemskap
        </Title>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <div>§ 18-3</div>
              <div>Barnet er medlem av trygden/bosatt i Norge fra dødsfalltidspunktet til i dag</div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Bostedadresse</strong>
              </div>
              <div>Adresse her</div>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>{vilkaarErOppfylt(props.vilkaar.resultat)}</VilkaarlisteTitle>
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
