import { IPdlPerson } from '~shared/types/Person'
import { PersonInfoAdresse } from './PersonInfoAdresse'
import { hentAdresserEtterDoedsdato, hentAlderForDato } from '~components/behandling/felles/utils'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { BodyShort, HStack, Label, VStack } from '@navikt/ds-react'

type Props = {
  person: IPdlPerson
  doedsdato: Date | undefined
}

export const Barn = ({ person, doedsdato }: Props) => {
  const bostedsadresse = person.bostedsadresse ?? []
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(bostedsadresse, doedsdato)
  const aktivAdresse = bostedsadresse.find((adresse) => adresse.aktiv)

  return (
    <HStack gap="space-16">
      <HStack gap="space-2">
        <ChildEyesIcon aria-hidden />
        <BodyShort>
          {person.fornavn} {person.etternavn}
        </BodyShort>
        <BodyShort>({hentAlderForDato(person.foedselsdato)} år)</BodyShort>
      </HStack>
      <VStack>
        <Label>Fødselsnummer</Label>
        <BodyShort>{person.foedselsnummer}</BodyShort>
      </VStack>
      <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} adresseDoedstidspunkt={false} />
      {aktivAdresse && (
        <VStack>
          <Label>Aktiv adresse</Label>
          <BodyShort>{aktivAdresse.adresseLinje1}</BodyShort>
          <BodyShort>{aktivAdresse.land}</BodyShort>
        </VStack>
      )}
    </HStack>
  )
}
