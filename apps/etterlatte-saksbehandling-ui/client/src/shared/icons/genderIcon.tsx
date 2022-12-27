import styled from 'styled-components'
import { Female } from '@navikt/ds-icons'
import { Male } from '@navikt/ds-icons'

export enum GenderList {
  male = 'male',
  female = 'female',
}

export const GenderIcon = (props: { gender: GenderList }) => {
  if (props.gender === GenderList.female) {
    return (
      <Gender gender={props.gender}>
        <Female color={'#fff'} />
      </Gender>
    )
  }

  return (
    <Gender gender={props.gender}>
      <Male color={'#fff'} />
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
