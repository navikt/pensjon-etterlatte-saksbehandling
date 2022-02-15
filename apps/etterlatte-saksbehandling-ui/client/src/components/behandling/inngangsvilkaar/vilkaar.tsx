import styled from 'styled-components'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import { IVilkaarProps, VilkaarType } from './types'

export const Vilkaar = (props: IVilkaarProps) => {
  console.log(props.vilkaar.vilkaarType)
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
        {props.vilkaar.vilkaarType === VilkaarType.doedsdato && (
          // Egne komponenter
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
        )}
        {props.vilkaar.vilkaarType === VilkaarType.soeker_foedselsdato && (
          <Title>
            <StatusIcon status={props.vilkaar.vilkaarDone} /> Sø
          </Title>
        )}
      </VilkaarWrapper>
    </div>
  )
}

const VilkaarWrapper = styled.div`
  height: 100px;
  padding: 1em 1em 1em 0;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
`

const Title = styled.div`
  display: flex;
  font-size: 1.1em;
  font-weight: bold;
`

const VilkaarColumn = styled.div``
