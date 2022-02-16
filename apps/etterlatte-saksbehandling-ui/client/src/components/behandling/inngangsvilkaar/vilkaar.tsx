import { Title, VilkaarWrapper } from './styled'
import { IVilkaarProps, VilkaarType } from './types'
import { AlderBarn } from './vilkaar/AlderBarn'
import { DoedsFallForelder } from './vilkaar/DoedsfallForelder'

export const Vilkaar = (props: IVilkaarProps) => {
  if (!props.vilkaar.grunnlag) {
    return (
      <div style={{ borderBottom: '1px solid #ccc' }}>
        <VilkaarWrapper>
          <Title>Mangler grunnlag</Title>
        </VilkaarWrapper>
      </div>
    )
  }

  return (
    <div style={{ borderBottom: '1px solid #ccc' }}>
      <VilkaarWrapper>
        {props.vilkaar.vilkaarType === VilkaarType.doedsdato && <DoedsFallForelder vilkaar={props.vilkaar} />}
        {props.vilkaar.vilkaarType === VilkaarType.soeker_foedselsdato && (
          <AlderBarn vilkaar={props.vilkaar} />
        )}
      </VilkaarWrapper>
    </div>
  )
}

