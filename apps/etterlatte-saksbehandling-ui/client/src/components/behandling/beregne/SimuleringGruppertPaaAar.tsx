import { Accordion, Box, Heading, Table, VStack } from '@navikt/ds-react'
import { getYear } from 'date-fns'
import { SimulertBeregning, SimulertBeregningsperiode } from '~shared/types/Utbetaling'
import { NOK } from '~utils/formatering/formatering'
import { summerEtterbetaling, summerFeilutbetaling, UtbetalingTable } from './UtbetalingTable'

const EtterbetalingRader = ({
  etterbetaling,
  suffix = '',
}: {
  etterbetaling: ReturnType<typeof summerEtterbetaling>
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Brutto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Skatt</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.skatt)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Netto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.netto)}</Table.DataCell>
    </Table.Row>
  </>
)

const FeilutbetalingRader = ({
  feilutbetaling,
  suffix = '',
}: {
  feilutbetaling: ReturnType<typeof summerFeilutbetaling>
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Brutto feilutbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Skatt</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.skatt)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Netto feilutbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.netto)}</Table.DataCell>
    </Table.Row>
  </>
)

export const SimuleringGruppertPaaAar = ({ data }: { data: SimulertBeregning }) => {
  const aarMedPerioder = grupperPerioderPerAar(data)

  return (
    <>
      {aarMedPerioder.map((aar) => (
        <Box key={aar.aarstall} maxWidth="70rem" background="neutral-soft" padding="space-20">
          <Heading level="3" size="small">
            Resultat av simulering i {aar.aarstall}
          </Heading>
          <Box width="25rem" marginBlock="space-20">
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
                <EtterbetalingRader etterbetaling={summerEtterbetaling(aar.etterbetaling)} />
                {aar.tilbakekreving.length > 0 && (
                  <FeilutbetalingRader feilutbetaling={summerFeilutbetaling(aar.etterbetaling, aar.tilbakekreving)} />
                )}
              </Table.Body>
            </Table>
          </Box>
          <Box maxWidth="1000px">
            <Accordion size="small">
              <Accordion.Item>
                <Accordion.Header>Se detaljer om simulering i {aar.aarstall}</Accordion.Header>
                <Accordion.Content>
                  <VStack gap="space-20">
                    <UtbetalingTable tittel={`Etterbetaling ${aar.aarstall}`} perioder={aar.etterbetaling} />
                    <UtbetalingTable tittel={`Tilbakekreving ${aar.aarstall}`} perioder={aar.tilbakekreving} />
                  </VStack>
                </Accordion.Content>
              </Accordion.Item>
            </Accordion>
          </Box>
        </Box>
      ))}
      {aarMedPerioder.length > 1 && (
        <Box maxWidth="70rem" background="neutral-soft" padding="space-20">
          <Heading level="3" size="small">
            Etterbetaling for hele perioden
          </Heading>
          <Box width="25rem" marginBlock="space-20">
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
                <EtterbetalingRader etterbetaling={summerEtterbetaling(data.etterbetaling)} suffix=" for perioden" />
                {data.tilbakekreving.length > 0 && (
                  <FeilutbetalingRader
                    feilutbetaling={summerFeilutbetaling(data.etterbetaling, data.tilbakekreving)}
                    suffix=" for perioden"
                  />
                )}
              </Table.Body>
            </Table>
          </Box>
        </Box>
      )}
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
