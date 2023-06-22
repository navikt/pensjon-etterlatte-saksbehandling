import styled from 'styled-components'
import { Female, Male } from '@navikt/ds-icons'

export enum GenderList {
  male = 'male',
  female = 'female',
}

export const GenderIcon = (props: { gender: GenderList }) => {
  return (
    <Gender gender={props.gender}>
      {props.gender === GenderList.female ? <Female color={'#fff'} /> : <Male color={'#fff'} />}
    </Gender>
  )
}

const Gender = styled.div<{ gender: GenderList }>`
  line-height: 30px;
  background-color: ${(props) => (props.gender === GenderList.female ? '#c86151' : 'blue')};
  padding: 3px;
  width: 30px;
  height: 30px;
  border-radius: 100%;
  text-align: center;
`
