import { StatusIcon } from "../../../../shared/icons/statusIcon"
import { Title, VilkaarColumn } from "../styled"
import { IVilkaarProps } from "../types"


export const DoedsFallForelder = (props: IVilkaarProps) => {
  return (
    <>
      <VilkaarColumn>
        <Title>
          <StatusIcon status={props.vilkaar.vilkaarDone} /> Dødsfall forelder
        </Title>
        <div>§ 18-5</div>
        <div>En eller begge foreldrene døde</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div>Dødsdato</div>
        <div>30.10.2021</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <div>Avdød forelder</div>
        <div>Ola Nilsen Normann</div>
        <div>090248 54688</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Vilkår er {props.vilkaar.vilkaarStatus}</Title>
      </VilkaarColumn>
    </>
  )
}