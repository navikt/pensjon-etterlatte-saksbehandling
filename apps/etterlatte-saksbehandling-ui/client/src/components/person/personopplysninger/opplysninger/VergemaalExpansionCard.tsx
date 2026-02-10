import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { PersonGroupIcon } from '@navikt/aksel-icons'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { lowerCase, upperFirst } from 'lodash'
import React from 'react'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'

export const VergemaalExpansionCard = ({ vergemaal }: { vergemaal?: VergemaalEllerFremtidsfullmakt[] }) => {
  return (
    <ExpansionCard aria-labelledby="Vergemål" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <PersonGroupIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">Vergemål</ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
              <Table.HeaderCell scope="col">Omfang</Table.HeaderCell>
              <Table.HeaderCell scope="col">Type</Table.HeaderCell>
              <Table.HeaderCell scope="col">Embete</Table.HeaderCell>
              <Table.HeaderCell scope="col">Opphørstidspunkt</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!vergemaal?.length ? (
              vergemaal.map((verge, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {verge.vergeEllerFullmektig.motpartsPersonident && verge.vergeEllerFullmektig.navn}
                  </Table.DataCell>
                  <Table.DataCell>
                    {!!verge.vergeEllerFullmektig.motpartsPersonident && (
                      <KopierbarVerdi value={verge.vergeEllerFullmektig.motpartsPersonident} iconPosition="right" />
                    )}
                  </Table.DataCell>
                  <Table.DataCell>{verge.vergeEllerFullmektig.omfang}</Table.DataCell>
                  <Table.DataCell>{!!verge.type && upperFirst(lowerCase(verge.type))}</Table.DataCell>
                  <Table.DataCell>{verge.embete}</Table.DataCell>
                  <Table.DataCell>
                    {formaterKanskjeStringDatoMedFallback('Ingen opphørstidspunkt', verge.opphoerstidspunkt)}
                  </Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={5}>
                  <Heading size="small">Ingen vergemål</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
