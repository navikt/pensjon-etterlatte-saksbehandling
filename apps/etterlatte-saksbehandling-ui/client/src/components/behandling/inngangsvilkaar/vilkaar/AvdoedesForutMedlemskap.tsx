import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from '../styled'
import { VilkaarProps } from '../types'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'
import { KildeDatoVilkaar } from './KildeDatoOpplysning'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Avdødes forutgående medlemskap</Title>
              <Lovtekst>
                § 18-2: Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet
              </Lovtekst>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <StatusIcon status={VurderingsResultat.OPPFYLT} large={true} /> Vilkår er oppfylt
              </VilkaarlisteTitle>
              <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
        <TidslinjeMedlemskap />
      </Innhold>
    </VilkaarBorder>
  )
}
