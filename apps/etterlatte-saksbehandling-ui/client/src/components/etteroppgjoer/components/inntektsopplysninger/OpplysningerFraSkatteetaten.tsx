import { Heading, HelpText, HStack, Table, VStack } from '@navikt/ds-react'
import { PensjonsgivendeInntektFraSkatteetatenSummert } from '~shared/types/EtteroppgjoerForbehandling'
import { NOK } from '~utils/formatering/formatering'
import React from 'react'

export const OpplysningerFraSkatteetaten = ({
  inntektFraSkatteetaten,
}: {
  inntektFraSkatteetaten: PensjonsgivendeInntektFraSkatteetatenSummert
}) => {
  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra Skatteetaten</Heading>

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Type inntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">Beløp</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          <Table.Row>
            <Table.DataCell>Lønnsinntekt</Table.DataCell>
            <Table.DataCell>{NOK(inntektFraSkatteetaten.loensinntekt)}</Table.DataCell>
          </Table.Row>
          <Table.Row>
            <Table.DataCell>
              <HStack gap="1">
                Næringsinntekt
                <HelpText>Næringsinntekt inkluderer også næringsinntekt fra fiske, fangst og familiebarnehage</HelpText>
              </HStack>
            </Table.DataCell>
            <Table.DataCell>
              {NOK(inntektFraSkatteetaten.naeringsinntekt + inntektFraSkatteetaten.fiskeFangstFamiliebarnehage)}
            </Table.DataCell>
          </Table.Row>
        </Table.Body>
      </Table>
    </VStack>
  )
}
