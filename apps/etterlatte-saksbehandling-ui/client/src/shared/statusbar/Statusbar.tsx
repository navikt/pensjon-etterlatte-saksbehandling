import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { HStack, Link } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { IPdlPersonNavnFoedsel } from '~shared/types/Person'
import { mapApiResult, Result } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { useEffect } from 'react'
import { AGray600 } from '@navikt/ds-tokens/dist/tokens'
import { differenceInYears, parse } from 'date-fns'
import { DatoFormat } from '~utils/formattering'

export const PdlPersonStatusBar = ({ person }: { person: IPdlPersonNavnFoedsel }) => (
  <StatusBar
    personResultStatus={{
      status: 'success',
      data: {
        foedselsnummer: person.foedselsnummer,
        fornavn: person.fornavn,
        mellomnavn: person.mellomnavn,
        etternavn: person.etternavn,
        foedselsaar: person.foedselsaar,
        foedselsdato: person.foedselsdato,
      },
    }}
  />
)

export const StatusBarPersonHenter = ({ ident }: { ident: string | null | undefined }) => {
  const [personStatus, hentPerson] = useApiCall(hentPersonNavnogFoedsel)
  useEffect(() => {
    ident && hentPerson(ident)
  }, [ident])

  return <StatusBar personResultStatus={personStatus} />
}

const StatusBar = ({ personResultStatus }: { personResultStatus: Result<IPdlPersonNavnFoedsel> }) => {
  const gender = (fnr: string): GenderList => {
    const genderNum = Number(fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return mapApiResult(
    personResultStatus,
    <PersonSkeleton />,
    () => null,
    (person) => (
      <StatusBarWrapper>
        <UserInfo>
          <GenderIcon gender={gender(person.foedselsnummer)} />
          <HStack gap="2">
            <Name>
              <Link href={`/person/${person.foedselsnummer}`}>{genererNavn(person)}</Link>
            </Name>
            <VisAlderForPerson foedselsaar={person.foedselsaar} foedselsdato={person.foedselsdato} />
          </HStack>
          <Skilletegn>|</Skilletegn>
          <KopierbarVerdi value={person.foedselsnummer} />
        </UserInfo>
      </StatusBarWrapper>
    )
  )
}

const VisAlderForPerson = ({ foedselsaar, foedselsdato }: { foedselsaar: number; foedselsdato: Date | undefined }) => {
  const idag = new Date()
  const alder = foedselsdato
    ? differenceInYears(idag, parse(String(foedselsdato), DatoFormat.AAR_MAANED_DAG, new Date()))
    : idag.getFullYear() - foedselsaar
  return <span style={{ color: AGray600, whiteSpace: 'pre-wrap' }}>({alder} år)</span>
}

const PersonSkeleton = () => (
  <StatusBarWrapper>
    <UserInfo>
      <SkeletonGenderIcon />
      <Name>
        <Skeleton />
      </Name>
      <Skilletegn>|</Skilletegn>
      <Skeleton />
    </UserInfo>
  </StatusBarWrapper>
)

const genererNavn = (personInfo: IPdlPersonNavnFoedsel) => {
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
