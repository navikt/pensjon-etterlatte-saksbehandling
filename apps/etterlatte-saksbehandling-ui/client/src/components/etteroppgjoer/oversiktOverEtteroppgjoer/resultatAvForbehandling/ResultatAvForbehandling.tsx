import { BodyShort, Box, Heading, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { UtfallAvForbehandling } from '~shared/types/Etteroppgjoer'
import { CheckmarkCircleIcon, EnvelopeClosedIcon } from '@navikt/aksel-icons'

export const ResultatAvForbehandling = ({
  utfallAvForbehandling,
}: {
  utfallAvForbehandling: UtfallAvForbehandling
}) => {
  return (
    <VStack gap="4">
      <Heading size="large">Resultat</Heading>
      <Box maxWidth="30rem">
        <Table>
          <Table.Header>
            <Table.HeaderCell scope="col">Utregning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Beløp</Table.HeaderCell>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.HeaderCell scope="row">Brutto utbetalt stønad</Table.HeaderCell>
              <Table.DataCell>{NOK(2)}</Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Ny brutto stønad</Table.HeaderCell>
              <Table.DataCell>{NOK(5)}</Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Differanse</Table.HeaderCell>
              <Table.DataCell>{NOK(2000)}</Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Grense</Table.HeaderCell>
              <Table.DataCell>{NOK(2000)}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </Box>

      {utfallAvForbehandling === UtfallAvForbehandling.SEND_VARSELBREV && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Resultat av forbehandlingen</Label>
            <BodyShort>Basert på behandlingen skal du nå sende varselbrev til brukeren.</BodyShort>
          </VStack>
        </HStack>
      )}
      {utfallAvForbehandling === UtfallAvForbehandling.SEND_INFORMASJONSBREV && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Resultat av forbehandlingen</Label>
            <BodyShort>Basert på behandlingen skal du nå sende informasjonsbrev til brukeren.</BodyShort>
          </VStack>
        </HStack>
      )}
      {utfallAvForbehandling === UtfallAvForbehandling.FERDIGSTILL_UTEN_ENDRING && (
        <HStack gap="2" maxWidth="fit-content">
          <CheckmarkCircleIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Resultat av forbehandlingen</Label>
            <BodyShort>
              Ettersom dette ikke resulterer i etterbetaling eller tilbakekreving og bruker har 0 utbetalt, trenger man
              ikke sende ut brev. Du kan nå ferdistille oppgaven.
            </BodyShort>
          </VStack>
        </HStack>
      )}
    </VStack>
  )
}
