import { ChildEyesIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from './PersonInfoAdresse'
import React from 'react'
import { hentAdresserEtterDoedsdato, hentAlderForDato } from '~components/behandling/felles/utils'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import { BodyShort, HStack, Label, VStack } from '@navikt/ds-react'
import { Personopplysning } from '~shared/types/grunnlag'

export const Soesken = ({ person, familieforhold }: { person: IPdlPerson; familieforhold: Familieforhold }) => {
  const avdoede = familieforhold.avdoede.find((po) => po)!

  const erHelsoesken = (soeker: Personopplysning | undefined, soesken: IPdlPerson) => {
    const foreldreSoeker = new Set(soeker?.opplysning.familieRelasjon?.foreldre ?? [])
    const foreldreSoesken = new Set(soesken.familieRelasjon?.foreldre ?? [])
    return foreldreSoeker.difference(foreldreSoesken).size === 0
  }

  return (
    <>
      <VStack>
        <HStack gap="space-2">
          <ChildEyesIcon aria-hidden />
          <BodyShort>
            {person.fornavn} {person.etternavn}
          </BodyShort>
          <BodyShort>({hentAlderForDato(person.foedselsdato)} år)</BodyShort>
        </HStack>
        <BodyShort>{erHelsoesken(familieforhold.soeker, person) ? 'Helsøsken' : 'Halvsøsken'}</BodyShort>
      </VStack>
      <VStack>
        <Label>Fødselsnummer</Label>
        <BodyShort>{person.foedselsnummer}</BodyShort>
      </VStack>
      <PersonInfoAdresse
        adresser={hentAdresserEtterDoedsdato(person.bostedsadresse!!, avdoede.opplysning.doedsdato)}
        visHistorikk={true}
        adresseDoedstidspunkt={false}
      />
    </>
  )
}
