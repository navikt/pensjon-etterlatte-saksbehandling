import { StatusIcon } from "../../../../shared/icons/statusIcon"
import { Title, VilkaarColumn } from "../styled"
import { IVilkaarProps } from "../types"

export const AlderBarn = (props: IVilkaarProps) => {
  return (
    <>
      <VilkaarColumn>
        <Title>
          <StatusIcon status={props.vilkaar.vilkaarDone} /> Alder barn
        </Title>
        <div>§ 18-5</div>
        <div>En eller begge foreldrene døde</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div>Barnets fødselsdato</div>
        <div>30.10.2021</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div>Alder ved dødsfall</div>
        <div>9 år</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Vilkår er {props.vilkaar.vilkaarStatus}</Title>
      </VilkaarColumn>
    </>
  )
}