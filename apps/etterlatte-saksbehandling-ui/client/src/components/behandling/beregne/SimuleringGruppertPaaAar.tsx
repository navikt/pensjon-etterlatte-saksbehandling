import { Accordion, Box, Heading, Table, VStack } from '@navikt/ds-react'
import { getYear } from 'date-fns'
import { SimulertBeregning, SimulertBeregningsperiode } from '~shared/types/Utbetaling'
import { NOK } from '~utils/formatering/formatering'
import { summerPerioder, UtbetalingTable } from './UtbetalingTable'

export const SimuleringGruppertPaaAar = ({ data }: { data: SimulertBeregning }) => {
  const aarMedPerioder = grupperPerioderPerAar(data)

  return (
    <>
      {aarMedPerioder.map((aar) => (
        <Box key={aar.aarstall} maxWidth="70rem" background="surface-subtle" padding="5">
          <Heading level="3" size="small">
            Resultat av simulering i {aar.aarstall}
          </Heading>
          <Box width="25rem" marginBlock="5">
            <Table>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                  <Table.HeaderCell scope="col" align="right">
                    Sum
                  </Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                <Table.Row>
                  <Table.DataCell>Etterbetaling</Table.DataCell>
                  <Table.DataCell align="right">{NOK(summerPerioder(aar.etterbetaling))}</Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.DataCell>Tilbakekreving</Table.DataCell>
                  <Table.DataCell align="right">{NOK(summerPerioder(aar.tilbakekreving))}</Table.DataCell>
                </Table.Row>
              </Table.Body>
            </Table>
          </Box>
          <Box maxWidth="1000px">
            <Accordion size="small">
              <Accordion.Item>
                <Accordion.Header>Se detaljer om simulering i {aar.aarstall}</Accordion.Header>
                <Accordion.Content>
                  <VStack gap="5">
                    <UtbetalingTable tittel={`Etterbetaling ${aar.aarstall}`} perioder={aar.etterbetaling} />
                    <UtbetalingTable tittel={`Tilbakekreving ${aar.aarstall}`} perioder={aar.tilbakekreving} />
                  </VStack>
                </Accordion.Content>
              </Accordion.Item>
            </Accordion>
          </Box>
        </Box>
      ))}
    </>
  )
}

function hentPerioderForAar(aarstall: number, perioder: SimulertBeregningsperiode[]) {
  return perioder.filter((periode) => {
    const aar = getYear(periode.fom)
    return aar == aarstall
  })
}

function grupperPerioderPerAar(data: SimulertBeregning) {
  const alleAarstall = [...data.etterbetaling, ...data.tilbakekreving]
    .map((betaling) => {
      return getYear(betaling.fom)
    })
    .sort((a, b) => b - a) // Nyeste årstall først

  const unikeAarstall = [...new Set(alleAarstall)]
  const aarMedPerioder = unikeAarstall.map((aarstall) => {
    return {
      aarstall: aarstall,
      etterbetaling: hentPerioderForAar(aarstall, data.etterbetaling),
      tilbakekreving: hentPerioderForAar(aarstall, data.tilbakekreving),
    }
  })

  return aarMedPerioder
}
