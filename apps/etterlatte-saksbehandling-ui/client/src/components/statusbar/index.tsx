import styled from 'styled-components'
import { GenderIcon, GenderList } from '../../shared/icons/genderIcon'
import { PersonInfo } from '../behandling/personopplysninger/PersonInfo'
import { Fnr } from './fnr'
import { PersonStatus, Status } from './status'

export enum StatusBarTheme {
  gray = 'gray',
  white = 'white',
}

export interface PersonInfo {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  type: string
}

export const StatusBar = (props: { theme?: StatusBarTheme; personInfo?: PersonInfo }) => {
  const gender = (): GenderList => {
    const genderNum = Number(props.personInfo?.foedselsnummer[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return (
    <StatusBarWrapper theme={props.theme}>
      {props.personInfo?.foedselsnummer && (
        <UserInfo>
          <GenderIcon gender={gender()} />
          <Name>
            {props.personInfo?.fornavn} {props.personInfo?.etternavn}
          </Name>
          <Skilletegn>/</Skilletegn>
          <Fnr copy value={props.personInfo?.foedselsnummer || 'N/A'} />
          <Status value={{ status: PersonStatus.ETTERLATT, dato: '19.05.2011' }} />
        </UserInfo>
      )}
    </StatusBarWrapper>
  )
}

const StatusBarWrapper = styled.div<{ theme: StatusBarTheme }>`
  background-color: ${(props) => (props.theme === StatusBarTheme.gray ? '#F8F8F8' : '#fff')};
  padding: 0.6em 1em;
  line-height: 30px;
  border-bottom: 1px solid #c6c2bf;
`

const UserInfo = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: baseline;
  width: 450px;
  margin-left: 1em;
`

const Skilletegn = styled.div`
  margin-right: 1em;
`

const Name = styled.div`
  font-weight: 600;
  margin-right: auto;
  margin-left: 0.5em;
`
