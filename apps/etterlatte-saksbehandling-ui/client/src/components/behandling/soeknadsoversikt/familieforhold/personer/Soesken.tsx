import { ChildEyesIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import React from 'react'
import { differenceInYears } from 'date-fns'
import { hentAdresserEtterDoedsdato } from '~components/behandling/felles/utils'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import { BodyShort, HStack, Label, VStack } from '@navikt/ds-react'

export const Soesken = ({ person, familieforhold }: { person: IPdlPerson; familieforhold: Familieforhold }) => {
  const avdoede = familieforhold.avdoede.find((po) => po)!
  const gjenlevende = familieforhold.gjenlevende?.find((po) => po)
  const erHelsoesken = (fnr: string) => gjenlevende?.opplysning.familieRelasjon?.barn?.includes(fnr)

  return (
    <>
      <VStack>
        <HStack gap="2">
          <ChildEyesIcon aria-hidden />
          <BodyShort>
            {person.fornavn} {person.etternavn}
          </BodyShort>
          <BodyShort>({differenceInYears(new Date(), new Date(person.foedselsdato))} år)</BodyShort>
        </HStack>
        <BodyShort>{erHelsoesken(person.foedselsnummer) ? 'Helsøsken' : 'Halvsøsken'}</BodyShort>
      </VStack>
      <VStack>
        <Label>Fødselsnummer</Label>
        <BodyShort>{person.foedselsnummer}</BodyShort>
      </VStack>
      <PersonInfoAdresse
        adresser={hentAdresserEtterDoedsdato(
          person.bostedsadresse!!,
          avdoede.opplysning.doedsdato ? avdoede.opplysning.doedsdato.toString() : 'Ingen dødsdato for avdød'
        )}
        visHistorikk={true}
        adresseDoedstidspunkt={false}
      />
    </>
  )
}
