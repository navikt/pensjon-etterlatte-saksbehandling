import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import {
  Innhold,
  Lovtekst,
  StatusColumn,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
} from '../styled'
import { VilkaarProps } from '../types'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'
import { AutomaticIcon } from '../../../../shared/icons/automaticIcon'
import { KildeDatoVilkaar } from './KildeDatoOpplysning'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  console.log(vilkaar)

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <StatusColumn>
            <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} large={true} />
          </StatusColumn>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Avdødes forutgående medlemskap</Title>
              <Lovtekst>
                § 18-2: Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet
              </Lovtekst>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>
              <AutomaticIcon /> Vilkår er oppfylt
            </VilkaarlisteTitle>
            <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
        <TidslinjeMedlemskap />
      </Innhold>
    </VilkaarBorder>
  )
}
