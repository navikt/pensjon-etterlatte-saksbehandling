import { StatusIcon } from '../../../../shared/icons/statusIcon'
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
import { vilkaarErOppfylt } from './utils'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title> § 18-2: Avdødes forutgående medlemskap</Title>
              <Lovtekst>
                Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet
              </Lovtekst>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            {vilkaar != undefined && (
              <VilkaarVurderingContainer>
                <VilkaarlisteTitle>
                  <StatusIcon status={vilkaar.resultat} large={true} /> {vilkaarErOppfylt(vilkaar.resultat)}
                </VilkaarlisteTitle>
                <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
              </VilkaarVurderingContainer>
            )}
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
        {vilkaar != undefined ? <TidslinjeMedlemskap vilkaar={vilkaar} /> : <div>Vilkår mangler</div>}
      </Innhold>
    </VilkaarBorder>
  )
}
