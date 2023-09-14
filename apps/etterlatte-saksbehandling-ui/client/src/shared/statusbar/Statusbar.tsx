import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { IPersonResult } from '~components/person/typer'
import { isPending, isSuccess, Result } from '~shared/hooks/useApiCall'
import { Link } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { IPdlPerson } from '~shared/types/Person'

export const PdlPersonStatusBar = ({ person }: { person: IPdlPerson }) => (
  <StatusBar
    result={{
      status: 'success',
      data: {
        foedselsnummer: person.foedselsnummer,
        fornavn: person.fornavn,
        mellomnavn: person.mellomnavn,
        etternavn: person.etternavn,
      },
    }}
  />
)

export const StatusBar = ({ result }: { result: Result<IPersonResult> }) => {
  const gender = (fnr: string): GenderList => {
    const genderNum = Number(fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return (
    <StatusBarWrapper>
      {isPending(result) && (
        <UserInfo>
          <SkeletonGenderIcon />
          <Name>
            <Skeleton />
          </Name>
          <Skilletegn>|</Skilletegn>
          <Skeleton />
        </UserInfo>
      )}
      {isSuccess(result) && (
        <UserInfo>
          <GenderIcon gender={gender(result.data.foedselsnummer)} />
          <Name>
            <Link href={`/person/${result.data.foedselsnummer}`}>{genererNavn(result.data)}</Link>
          </Name>
          <Skilletegn>|</Skilletegn>
          <KopierbarVerdi value={result.data.foedselsnummer} />
        </UserInfo>
      )}
    </StatusBarWrapper>
  )
}

const genererNavn = (personInfo: IPersonResult) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
}

const StatusBarWrapper = styled.div`
  background-color: #f8f8f8;
  padding: 0.6em 0;
  line-height: 2rem;
  border-bottom: 1px solid #c6c2bf;
`

const UserInfo = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
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

const Skeleton = styled.div`
  background: linear-gradient(-45deg, #bebebe 40%, #d3d3d3 60%, #bebebe 80%);
  border-radius: 1rem;
  width: 10rem;
  height: 1rem;
  margin-left: 1rem;
`

const SkeletonGenderIcon = styled.div`
  line-height: 30px;
  background-color: gray;
  padding: 3px;
  width: 30px;
  height: 30px;
  border-radius: 100%;
`
