import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
} from '../styled'
import { VilkaarProps } from '../types'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  console.log(vilkaar)

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} large={true} /> Avdødes forutgående medlemskap
        </Title>
        <Lovtekst>
          § 18-2: Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet
        </Lovtekst>
        <TidslinjeMedlemskap />
        <VilkaarWrapper>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>Vilkår er oppfylt</VilkaarlisteTitle>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
