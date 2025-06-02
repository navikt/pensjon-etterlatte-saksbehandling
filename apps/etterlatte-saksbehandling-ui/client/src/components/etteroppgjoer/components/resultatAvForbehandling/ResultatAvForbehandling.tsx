import { Box, Heading, HelpText, HStack, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

export const ResultatAvForbehandling = () => {
  const etteroppgjoer = useEtteroppgjoer()

  if (!etteroppgjoer || !etteroppgjoer.beregnetEtteroppgjoerResultat) {
    return null
  }

  const resultat = etteroppgjoer?.beregnetEtteroppgjoerResultat

  return (
    <VStack gap="4">
      <Heading size="large">
        <HStack gap="2">Resultat</HStack>
      </Heading>
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
                <HStack justify="end">{NOK(resultat.nyBruttoStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">Brutto utbetalt stønad</Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">{NOK(resultat.utbetaltStoenad)}</HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">
                <HStack gap="2">
                  Avviksbeløp +/-
                  <HelpText>
                    {resultat.differanse > 0
                      ? 'Avviksbeløpet viser at det er utbetalt for mye.'
                      : resultat.differanse < 0
                        ? 'Avviksbeløpet viser at det er utbetalt for lite.'
                        : 'Avviksbeløpet viser at utbetalingen har vært korrekt.'}
                  </HelpText>
                </HStack>
              </Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">
                  {resultat.differanse > 0 && '+'}
                  {NOK(resultat.differanse)}
                </HStack>
              </Table.DataCell>
            </Table.Row>
            <Table.Row>
              <Table.HeaderCell scope="row">
                <HStack gap="2">
                  Toleransegrense
                  <HelpText>
                    Etteroppgjør skal unnlates hvis for lite utbetalt er mindre enn 25 prosent av rettsgebyret, eller
                    hvis for mye utbetalt er mindre enn ett rettsgebyr. Jf. forskriftens § 9.
                  </HelpText>
                </HStack>
              </Table.HeaderCell>
              <Table.DataCell>
                <HStack justify="end">
                  {resultat.differanse > 0 ? NOK(resultat.grense.tilbakekreving) : NOK(resultat.grense.etterbetaling)}
                </HStack>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>

        <p>
          {(() => {
            const diff = resultat.differanse
            const diffBeloep = NOK(Math.abs(diff))

            if (diff > 0) {
              if (diff <= resultat.grense.tilbakekreving) {
                return `Resultatet viser at det er utbetalt ${diffBeloep} for mye stønad i ${etteroppgjoer.behandling.aar}, men beløpet er innenfor toleransegrense for tilbakekreving, og det kreves derfor ikke tilbake.`
              }
              return `Resultatet viser at det er utbetalt ${diffBeloep} for mye stønad i ${etteroppgjoer.behandling.aar}. Beløpet blir derfor krevd tilbake.`
            }

            if (diff < 0) {
              if (diff >= -resultat.grense.etterbetaling) {
                return `Resultatet viser at det er utbetalt ${diffBeloep} for lite stønad i${etteroppgjoer.behandling.aar}, men beløpet er innenfor toleransegrense for etterbetaling, og det blir derfor ikke utbetalt.`
              }
              return `Resultatet viser at det er utbetalt ${diffBeloep} for lite stønad i ${etteroppgjoer.behandling.aar}. Beløpet blir derfor etterbetalt.`
            }

            return `Resultatet viser ingen endring, bruker fikk utbetalt rett stønad i ${etteroppgjoer.behandling.aar}.`
          })()}
        </p>
      </Box>
    </VStack>
  )
}
