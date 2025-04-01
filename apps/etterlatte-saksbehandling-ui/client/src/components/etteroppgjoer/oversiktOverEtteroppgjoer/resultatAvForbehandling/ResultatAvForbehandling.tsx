import { BodyShort, Box, Heading, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { EtteroppgjoerResultatType } from '~shared/types/Etteroppgjoer'
import { CheckmarkCircleIcon, EnvelopeClosedIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'

export const ResultatAvForbehandling = () => {
  const { resultat } = useAppSelector((state) => state.etteroppgjoerReducer)

  if (!resultat) {
    return null
  }

  const utfallAvForbehandling = resultat.resultatType

  return (
    <VStack gap="4">
      <Heading size="large">Resultat</Heading>
      <Box maxWidth="25rem">
        <Table>
          <Table.Header>
            <Table.HeaderCell scope="col">Utregning</Table.HeaderCell>
            <Table.HeaderCell scope="col">
              <HStack justify="center">Beløp</HStack>
            </Table.HeaderCell>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.HeaderCell scope="row">Brutto utbetalt stønad</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">{NOK(resultat.utbetaltStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Ny brutto stønad</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">{NOK(resultat.nyBruttoStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Differanse +/-</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">+ {NOK(resultat.differanse)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Grense</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">
                  {resultat.differanse > 0 ? NOK(resultat.grense.etterbetaling) : NOK(resultat.grense.tilbakekreving)}
                </HStack>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </Box>

      {utfallAvForbehandling === EtteroppgjoerResultatType.TILBAKREVING && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Resultat av forbehandlingen</Label>
            <BodyShort>Basert på behandlingen skal du nå sende varselbrev om tilbakekreving til brukeren.</BodyShort>
          </VStack>
        </HStack>
      )}
      {utfallAvForbehandling === EtteroppgjoerResultatType.ETTERBETALING && (
        <HStack gap="2" maxWidth="fit-content">
          <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
          <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
            <Label>Resultat av forbehandlingen</Label>
            <BodyShort>Basert på behandlingen skal du nå sende varselbrev om etterbetaling til brukeren.</BodyShort>
          </VStack>
        </HStack>
      )}
      {utfallAvForbehandling === EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER && (
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
