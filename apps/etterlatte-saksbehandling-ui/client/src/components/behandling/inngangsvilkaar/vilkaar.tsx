import { Title, VilkaarWrapper } from './styled'
import { IVilkaarProps, OpplysningsType } from './types'
import { AlderBarn } from './vilkaar/AlderBarn'
import { AvdoedesForutMedlemskap } from './vilkaar/AvdoedesForutMedlemskap'
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
    <div style={{ borderBottom: '1px solid #ccc', padding: "1em 0" }}>
      <VilkaarWrapper>
        {props.vilkaar.vilkaarType === OpplysningsType.doedsdato && <DoedsFallForelder vilkaar={props.vilkaar} />}
        {props.vilkaar.vilkaarType === OpplysningsType.soeker_foedselsdato && (
          <AlderBarn vilkaar={props.vilkaar} />
        )}
        {props.vilkaar.vilkaarType === OpplysningsType.avdoedes_forutgaaende_medlemsskap && (
        <AvdoedesForutMedlemskap vilkaar={props.vilkaar} />
        )}
      </VilkaarWrapper>
    </div>
  )
}

