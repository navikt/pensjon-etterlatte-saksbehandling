import { GenderIcon, GenderList } from '../icons/genderIcon'
import { IPersonResult } from '~components/person/typer'
import { Alert, BodyShort, HelpText, HStack, Label, Skeleton } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { mapResult } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'
import { hentPersonNavnOgFoedsel } from '~shared/api/pdltjenester'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { differenceInYears } from 'date-fns'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { VergemaalTag } from '~shared/tags/VergemaalTag'
import { Navbar } from '~shared/header/Navbar'
import { useAppDispatch } from '~store/Store'
import { settPerson } from '~store/reducers/PersonReducer'
import { PersonOversiktFane } from '~components/person/Person'
import { ClickEvent } from '~utils/analytics'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { AdressebeskyttelseGraderingTag } from '~shared/tags/AdressebeskyttelseGraderingTag'

export const StatusBar = ({ ident }: { ident: string | null | undefined }) => {
  const dispatch = useAppDispatch()

  const [hentPersonNavnOgFoedselResult, hentPersonNavnOgFoedselFetch] = useApiCall(hentPersonNavnOgFoedsel)
  const [hentSakMedBehandlingerResult, hentSakMedBehandlingerFetch] = useApiCall(hentSakMedBehandlnger)

  useEffect(() => {
    if (ident) {
      hentPersonNavnOgFoedselFetch(ident, (person) => {
        dispatch(settPerson(person))
        hentSakMedBehandlingerFetch(person.foedselsnummer)
      })
    }
  }, [ident])

  const gender = (fnr: string): GenderList => {
    const genderNum = Number(fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return (
    <Navbar>
      {mapResult(hentPersonNavnOgFoedselResult, {
        pending: <PersonSkeleton />,
        error: (error) => <ApiErrorAlert>{error.detail ?? 'Kunne ikke hente person'}</ApiErrorAlert>,
        success: (person) => (
          <HStack gap="space-2" align="center" justify="start">
            <GenderIcon gender={gender(person.foedselsnummer)} />
            <Label>
              <PersonLink fnr={person.foedselsnummer}>{genererNavn(person)}</PersonLink>
            </Label>

            <DoedsdatoTag doedsdato={person.doedsdato} />

            <Alder foedselsdato={person.foedselsdato} doedsdato={person.doedsdato} foedselsaar={person.foedselsaar} />

            <BodyShort>|</BodyShort>
            <Label>
              <PersonLink
                fnr={person.foedselsnummer}
                fane={PersonOversiktFane.PERSONOPPLYSNINGER}
                clickEvent={ClickEvent.AAPNE_PERSONOPPLYSNINGER_FRA_STATUS_BAR}
                target="_blank"
              >
                Personopplysninger
              </PersonLink>
            </Label>
            <BodyShort>|</BodyShort>
            <KopierbarVerdi value={person.foedselsnummer} />
            {ident !== person.foedselsnummer && person.historiskeFoedselsnummer.includes(ident!) && (
              <Alert variant="warning" size="small">
                Bruker har historisk ident {ident}
              </Alert>
            )}

            <VergemaalTag vergemaal={person.vergemaal} />
            {mapResult(hentSakMedBehandlingerResult, {
              success: (sakMedBehandlinger) =>
                !!sakMedBehandlinger.sak.adressebeskyttelse && (
                  <AdressebeskyttelseGraderingTag
                    adressebeskyttelse={sakMedBehandlinger.sak.adressebeskyttelse}
                    size="small"
                  />
                ),
            })}
          </HStack>
        ),
      })}
    </Navbar>
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
  <HStack gap="space-4" align="center">
    <Skeleton variant="circle" width="30px" height="30px" />
    <Skeleton variant="rounded" width="10rem" height="1rem" />
    <BodyShort>|</BodyShort>
    <Skeleton variant="rounded" width="10rem" height="1rem" />
  </HStack>
)

const genererNavn = (personInfo: IPersonResult) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
}
