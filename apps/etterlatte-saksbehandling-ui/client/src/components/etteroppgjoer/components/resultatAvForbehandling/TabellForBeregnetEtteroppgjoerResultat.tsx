import { BodyLong, Box, Heading, HelpText, HStack, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'

export const TabellForBeregnetEtteroppgjoerResultat = () => {
  const { beregnetEtteroppgjoerResultat } = useEtteroppgjoerForbehandling()

  if (!beregnetEtteroppgjoerResultat) {
    return null
  }

  return (
    <VStack gap="space-4">
      <Heading size="large">
        <HStack gap="space-2">Resultat</HStack>
      </Heading>
      <Box maxWidth="42.5rem">
        <BodyLong>
          Tabellen viser beregningen av etteroppgjøret ut fra de opplysningene som er lagt inn. Sjekk at du har lagt inn
          riktig inntekt for perioden omstillingsstønaden er innvilget/utbetalt.
        </BodyLong>
      </Box>
      <Box maxWidth="25rem">
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Utregning</Table.HeaderCell>
              <Table.HeaderCell scope="col">
                <HStack justify="center">Beløp</HStack>
              </Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.HeaderCell scope="row">Ny brutto stønad</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">{NOK(beregnetEtteroppgjoerResultat.nyBruttoStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Brutto utbetalt stønad</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">{NOK(beregnetEtteroppgjoerResultat.utbetaltStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">
                <HStack gap="space-2">
                  {beregnetEtteroppgjoerResultat.differanse > 0
                    ? 'For mye utbetalt'
                    : beregnetEtteroppgjoerResultat.differanse < 0
                      ? 'For lite utbetalt'
                      : 'Riktig beløp utbetalt'}
                </HStack>
              </Table.HeaderCell>
              <Table.DataCell>
                {/* Vi vil kun vise tallet uten fortegn for saksbehandler, men det kan komme som negativt tall fra backend */}
                <HStack justify="end">{NOK(Math.abs(beregnetEtteroppgjoerResultat.differanse))}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">
                <HStack gap="space-2">
                  Toleransegrense
                  <HelpText>
                    Etteroppgjør skal unnlates hvis for lite utbetalt er mindre enn 25 prosent av rettsgebyret, eller
                    hvis for mye utbetalt er mindre enn ett rettsgebyr. Jf. forskriftens § 9.
                  </HelpText>
                </HStack>
              </Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">
                  {beregnetEtteroppgjoerResultat.differanse > 0
                    ? NOK(beregnetEtteroppgjoerResultat.grense.tilbakekreving)
                    : NOK(beregnetEtteroppgjoerResultat.grense.etterbetaling)}
                </HStack>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
