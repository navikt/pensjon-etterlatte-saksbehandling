import { BodyShort, Box, Heading, HelpText, HStack, Table, VStack } from '@navikt/ds-react'
import { PensjonsgivendeInntektFraSkatteetatenSummert } from '~shared/types/EtteroppgjoerForbehandling'
import { NOK } from '~utils/formatering/formatering'
import React from 'react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'

export const OpplysningerFraSkatteetaten = ({
  inntektFraSkatteetatenSummert,
}: {
  inntektFraSkatteetatenSummert: PensjonsgivendeInntektFraSkatteetatenSummert
}) => {
  const etteroppgjoer = useEtteroppgjoerForbehandling()

  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra Skatteetaten</Heading>
      <BodyShort>Pensjonsgivende inntekt for {etteroppgjoer.behandling.aar}.</BodyShort>

      <Box width="25rem">
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Type inntekt</Table.HeaderCell>
              <Table.HeaderCell scope="col" align="right">
                Beløp
              </Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.DataCell>
                <HStack gap="1">
                  Lønnsinntekt
                  <HelpText>Lønnsinntekt inkluderer også omstillingsstønad</HelpText>
                </HStack>
              </Table.DataCell>
              <Table.DataCell align="right">{NOK(inntektFraSkatteetatenSummert.loensinntekt)}</Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.DataCell>
                <HStack gap="1">
                  Næringsinntekt
                  <HelpText>
                    Næringsinntekt inkluderer også næringsinntekt fra fiske, fangst og familiebarnehage
                  </HelpText>
                </HStack>
              </Table.DataCell>
              <Table.DataCell align="right">{NOK(inntektFraSkatteetatenSummert.naeringsinntekt)}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
