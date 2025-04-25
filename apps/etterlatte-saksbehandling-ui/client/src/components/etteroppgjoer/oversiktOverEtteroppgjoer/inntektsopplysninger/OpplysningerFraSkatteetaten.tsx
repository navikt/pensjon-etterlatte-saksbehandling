import { Heading, Table, VStack } from '@navikt/ds-react'
import { PensjonsgivendeInntektFraSkatteetaten } from '~shared/types/Etteroppgjoer'
import { NOK } from '~utils/formatering/formatering'
import React from 'react'

export const OpplysningerFraSkatteetaten = ({
  pensjonsgivendeInntektFraSkatteetaten,
}: {
  pensjonsgivendeInntektFraSkatteetaten: PensjonsgivendeInntektFraSkatteetaten
}) => {
  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra Skatteetaten</Heading>

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Skatteordning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Lønnsinntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">Næringsinntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fiske, fangst og familiebarnehage</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!pensjonsgivendeInntektFraSkatteetaten.inntekter?.length ? (
            pensjonsgivendeInntektFraSkatteetaten.inntekter.map((inntekt, i) => (
              <Table.Row key={i}>
                <Table.DataCell>{inntekt.skatteordning}</Table.DataCell>
                <Table.DataCell>{NOK(inntekt.loensinntekt)}</Table.DataCell>
                <Table.DataCell>{NOK(inntekt.naeringsinntekt)}</Table.DataCell>
                <Table.DataCell>{NOK(inntekt.annet)}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={4}>
                <Heading size="small">Ingen inntekt fra skatt</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
