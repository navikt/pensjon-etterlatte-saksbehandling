import { Familiemedlem } from '~shared/types/familieOpplysninger'
import { BodyShort, ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { ChildHairEyesIcon } from '@navikt/aksel-icons'
import React from 'react'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'

export const AvdoedesBarnExpansionCard = ({ avdoede }: { avdoede?: Familiemedlem[] }) => {
  // Filtrerer bort duplikate søsken
  const avdoedesBarn = avdoede
    ?.flatMap((avdoed) => avdoed.barn ?? [])
    .filter((b, index, arr) => index === arr.findIndex((t) => t?.foedselsnummer === b.foedselsnummer))

  return (
    <ExpansionCard aria-labelledby="Avdødes barn" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <ChildHairEyesIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">Avdødes barn</ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
              <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!avdoede?.length ? (
              !!avdoedesBarn?.length ? (
                avdoedesBarn.map((barn, index) => (
                  <Table.Row key={index}>
                    <Table.DataCell>
                      <HStack gap="space-2" justify="start" align="center" wrap={false}>
                        <BodyShort>
                          {barn.fornavn} {barn.etternavn}
                        </BodyShort>
                        {!!barn.foedselsdato && <AlderTag foedselsdato={barn.foedselsdato} />}
                        <DoedsdatoTag doedsdato={barn.doedsdato} />
                      </HStack>
                    </Table.DataCell>
                    <Table.DataCell>
                      <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                    </Table.DataCell>
                    <BostedsadresseDataCell bostedsadresser={barn.bostedsadresse} index={index} />
                  </Table.Row>
                ))
              ) : (
                <Table.Row>
                  <Table.DataCell colSpan={3}>
                    <Heading size="small">Ingen barn</Heading>
                  </Table.DataCell>
                </Table.Row>
              )
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={3}>
                  <Heading size="small">Ingen avdøde</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
