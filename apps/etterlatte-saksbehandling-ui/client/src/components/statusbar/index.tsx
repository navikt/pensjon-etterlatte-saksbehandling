import styled from 'styled-components'
import { GenderIcon, GenderList } from '../../shared/icons/genderIcon'
import { Fnr } from './fnr'
import { IPersonInfo } from '../person/typer'

export enum StatusBarTheme {
  gray = 'gray',
  white = 'white',
}

export const StatusBar = (props: { theme?: StatusBarTheme; personInfo?: IPersonInfo }) => {
  const gender = (): GenderList => {
    const genderNum = Number(props.personInfo?.fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return (
    <StatusBarWrapper theme={props.theme}>
      {props.personInfo?.fnr && (
        <UserInfo>
          <GenderIcon gender={gender()} />
          <Name>{props.personInfo?.navn}</Name>
          <Skilletegn>|</Skilletegn>
          <Fnr copy value={props.personInfo?.fnr || 'N/A'} />
          {/** <Status value={{ status: PersonStatus.BARN, dato: '19.05.2011' }} />*/}
        </UserInfo>
      )}
    </StatusBarWrapper>
  )
}

const StatusBarWrapper = styled.div<{ theme: StatusBarTheme }>`
  background-color: ${(props) => (props.theme === StatusBarTheme.gray ? '#F8F8F8' : '#fff')};
  padding: 0.6em 0em;
  line-height: 30px;
  border-bottom: 1px solid #c6c2bf;
`

const UserInfo = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: baseline;
  width: fit-content;
  margin-left: 1em;
`

const Skilletegn = styled.div`
  margin-right: 1em;
  margin-left: 1em;
`

const Name = styled.div`
  font-weight: 600;
  margin-right: auto;
  margin-left: 0.5em;
`
