import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import {
  Innhold,
  Title,
  VilkaarBorder,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
  VilkaarColumn,
} from '../styled'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'

export const AvdoedesForutMedlemskap = (props: any) => {
  const vilkaar = props.vilkaar
  console.log(vilkaar)

  return (
    <VilkaarBorder>
      <Innhold>
        <Title>
          <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} large={true} /> Avdødes forutgående medlemskap
        </Title>
        <VilkaarWrapper>
          <VilkaarColumn>
            <div>§ 18-5</div>
            <div>Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet</div>
          </VilkaarColumn>
        </VilkaarWrapper>

        <TidslinjeMedlemskap />
        <VilkaarWrapper>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>Vilkår er ikke oppfylt</VilkaarlisteTitle>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
