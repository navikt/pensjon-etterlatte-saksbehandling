import { StatusIcon } from "../../../../shared/icons/statusIcon"
import { Title, VilkaarColumn } from "../styled"
import { IVilkaarProps } from "../types"

export const AlderBarn = (props: IVilkaarProps) => {
  const grunnlag = props.vilkaar.grunnlag;
  return (
    <>
      <VilkaarColumn>
        <Title>
          <StatusIcon status={props.vilkaar.vilkaarDone} /> Alder barn
        </Title>
        <div>§ 18-5</div>
        <div>Barnet er under 20 år</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div><strong>Barnets fødselsdato</strong></div>
        <div>{grunnlag.opplysning.foedselsdato}</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div><strong>Alder ved dødsfall</strong></div>
        <div>9 år</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Vilkår er {props.vilkaar.vilkaarStatus}</Title>
      </VilkaarColumn>
    </>
  )
}