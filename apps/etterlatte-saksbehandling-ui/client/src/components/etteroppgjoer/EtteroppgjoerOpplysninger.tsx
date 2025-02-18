import { Box, Heading, Table, VStack } from '@navikt/ds-react'
import React from 'react'
import { IEtteroppgjoerOpplysninger } from '~shared/types/Etteroppgjoer'
import { NOK } from '~utils/formatering/formatering'

export const EtteroppgjoerOpplysninger = ({ opplysninger }: { opplysninger: IEtteroppgjoerOpplysninger }) => {
  return (
    <>
      <VStack paddingBlock="8" paddingInline="16 8">
        <Heading size="small">Opplysninger skatt</Heading>
        <Box>Årsinntekt: {NOK(opplysninger.skatt.aarsinntekt)}</Box>
      </VStack>
      <VStack maxWidth="30rem" paddingBlock="8" paddingInline="16 8">
        <Heading size="small">Opplysninger A-Inntekt</Heading>
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Måned</Table.HeaderCell>
              <Table.HeaderCell>inntekt</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {opplysninger.ainntekt.inntektsmaaneder.map((maaned, i) => {
              return (
                <Table.Row key={i}>
                  <Table.DataCell>{maaned.maaned}</Table.DataCell>
                  <Table.DataCell>{NOK(maaned.summertBeloep)}</Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
      </VStack>
    </>
  )
}
