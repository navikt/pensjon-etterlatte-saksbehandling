import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { KopierbarVerdi } from './kopierbarVerdi'
import { Link } from '@navikt/ds-react'
import { IPersonResult } from '~components/person/typer'

export enum StatusBarTheme {
  gray = 'gray',
  white = 'white',
}

export const StatusBar = ({ theme, personInfo }: { theme: StatusBarTheme; personInfo: IPersonResult }) => {
  const gender = (): GenderList => {
    const genderNum = Number(personInfo.foedselsnummer[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  const navn = genererNavn(personInfo)

  return (
    <StatusBarWrapper theme={theme}>
      {personInfo.foedselsnummer && (
        <UserInfo>
          <GenderIcon gender={gender()} />
          <Name>
            <Link href={`/person/${personInfo.foedselsnummer}`}>{navn}</Link>{' '}
          </Name>
          <Skilletegn>|</Skilletegn>
          <KopierbarVerdi copy value={personInfo.foedselsnummer} />
          <Skilletegn>| Sakid </Skilletegn>
          <KopierbarVerdi copy value={personInfo.sakId.toString()} />
        </UserInfo>
      )}
    </StatusBarWrapper>
  )
}

const genererNavn = (personInfo: IPersonResult) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
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
  margin-left: 1em;
`

const Name = styled.div`
  font-weight: 600;
  margin-right: auto;
  margin-left: 0.5em;
`
