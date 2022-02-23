import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { Innhold, Title, VilkaarBorder, VilkaarColumn, VilkaarWrapper } from '../styled'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'

export const AvdoedesForutMedlemskap = (props: any) => {
  const vilkaar = props.vilkaar
  console.log(vilkaar)

  return (
    <VilkaarBorder>
      <Innhold>
        <Title>
          <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} /> Avdødes forutgående medlemskap
        </Title>
        <div>§ 18-5</div>
        <div>Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet</div>
        <TidslinjeMedlemskap />
        <VilkaarWrapper>
          <VilkaarColumn>
            <Title>Vilkår er ikke oppfyllt</Title>
          </VilkaarColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
