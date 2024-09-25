import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { IPersonResult } from '~components/person/typer'
import { BodyShort, Box, HelpText, HStack, Label, Skeleton } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { mapApiResult } from '~shared/api/apiUtils'
import { useEffect } from 'react'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { differenceInYears } from 'date-fns'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { VergemaalTag } from '~shared/tags/VergemaalTag'

export const StatusBar = ({ ident }: { ident: string | null | undefined }) => {
  const [result, hentPerson] = useApiCall(hentPersonNavnogFoedsel)

  useEffect(() => {
    ident && hentPerson(ident)
  }, [ident])

  const gender = (fnr: string): GenderList => {
    const genderNum = Number(fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return mapApiResult(
    result,
    <PersonSkeleton />,
    () => null,
    (person) => (
      <StatusbarBox>
        <HStack gap="2" align="center" justify="start">
          <GenderIcon gender={gender(person.foedselsnummer)} />
          <Label>
            <PersonLink fnr={person.foedselsnummer}>{genererNavn(person)}</PersonLink>
          </Label>

          <DoedsdatoTag doedsdato={person.doedsdato} />

          <Alder foedselsdato={person.foedselsdato} doedsdato={person.doedsdato} foedselsaar={person.foedselsaar} />

          <BodyShort>|</BodyShort>
          <KopierbarVerdi value={person.foedselsnummer} />

          <VergemaalTag vergemaal={person.vergemaal} />
        </HStack>
      </StatusbarBox>
    )
  )
}

const finnAlder = (foedselsdato: Date, doedsdato?: Date) => {
  if (doedsdato) {
    return differenceInYears(doedsdato, foedselsdato)
  } else {
    return hentAlderForDato(foedselsdato)
  }
}

const Alder = ({
  foedselsdato,
  doedsdato,
  foedselsaar,
}: {
  foedselsdato?: Date
  doedsdato?: Date
  foedselsaar: number
}) => {
  if (foedselsdato) {
    const alder = finnAlder(foedselsdato, doedsdato)
    return <BodyShort textColor="subtle">({alder} år)</BodyShort>
  } else {
    return (
      <>
        <BodyShort textColor="subtle">Fødselsår: {foedselsaar}</BodyShort>
        <HelpText title="Personen mangler fødselsdato">
          Vi har ingen fødselsdato på vedkommende og kan derfor ikke vise nøyaktig alder. Fødselsår: {foedselsaar}
        </HelpText>
      </>
    )
  }
}

const PersonSkeleton = () => (
  <StatusbarBox>
    <HStack gap="4" align="center">
      <Skeleton variant="circle" width="30px" height="30px" />
      <Skeleton variant="rounded" width="10rem" height="1rem" />
      <BodyShort>|</BodyShort>
      <Skeleton variant="rounded" width="10rem" height="1rem" />
    </HStack>
  </StatusbarBox>
)

const genererNavn = (personInfo: IPersonResult) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
}

const StatusbarBox = styled(Box)`
  padding: var(--a-spacing-3) 0 var(--a-spacing-3) var(--a-spacing-5);
  border-bottom: 1px solid var(--a-border-subtle);
  background: #f8f8f8;
`
