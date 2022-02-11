import styled from 'styled-components'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import { IVilkaarProps } from './types'

export const Vilkaar = (props: IVilkaarProps) => {
  return (
    <div style={{ borderBottom: '1px solid #ccc' }}>
      <VilkaarWrapper>
        <Title>
          <StatusIcon status={props.vilkaar.vilkaarDone} /> Dødsfall
        </Title>
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
`
