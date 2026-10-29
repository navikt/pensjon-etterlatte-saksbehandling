import { Box, Heading, LocalAlert, ReadMore, Table, VStack } from '@navikt/ds-react'
import { getYear } from 'date-fns'
import { CSSProperties, ReactNode } from 'react'
import {
  SimulertBeregning,
  SimulertBeregningsperiode,
  SimulertEtterbetalingOppsummering,
  SimulertFeilutbetalingOppsummering,
} from '~shared/types/Utbetaling'
import { NOK } from '~utils/formatering/formatering'
import { UtbetalingTable } from './UtbetalingTable'

const utenBunnlinje: CSSProperties = { borderBottom: 'none' }

const SumTabell = ({ children }: { children: ReactNode }) => (
  <Table>
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell scope="col">Type</Table.HeaderCell>
        <Table.HeaderCell scope="col" align="right">
          Sum
        </Table.HeaderCell>
      </Table.Row>
    </Table.Header>
    <Table.Body>{children}</Table.Body>
  </Table>
)

const EtterbetalingRader = ({
  etterbetaling,
  suffix = '',
}: {
  etterbetaling: SimulertEtterbetalingOppsummering
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Brutto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Skattetrekk</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.skatt)}</Table.DataCell>
    </Table.Row>
    {etterbetaling.andreTrekk !== 0 && (
      <Table.Row>
        <Table.DataCell>Andre trekk</Table.DataCell>
        <Table.DataCell align="right">{NOK(etterbetaling.andreTrekk)}</Table.DataCell>
      </Table.Row>
    )}
    <Table.Row>
      <Table.DataCell style={utenBunnlinje}>Netto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right" style={utenBunnlinje}>
        {NOK(etterbetaling.netto)}
      </Table.DataCell>
    </Table.Row>
  </>
)

const FeilutbetalingRader = ({
  feilutbetaling,
  suffix = '',
}: {
  feilutbetaling: SimulertFeilutbetalingOppsummering
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Tidligere utbetalt brutto{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Beløp brukeren skulle hatt{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.beloepBrukerenSkulleHatt)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell style={utenBunnlinje}>Brutto feilutbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right" style={utenBunnlinje}>
        {NOK(feilutbetaling.netto)}
      </Table.DataCell>
    </Table.Row>
  </>
)

export const SimuleringGruppertPaaAar = ({ data }: { data: SimulertBeregning }) => {
  const { oppsummeringer } = data

  if (!oppsummeringer) {
    return (
      <VStack gap="space-20">
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Oppsummering av simuleringen er ikke tilgjengelig</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            Eventuelle etterbetalinger og tilbakekrevinger vises i detaljtabellene.
          </LocalAlert.Content>
        </LocalAlert>
        {data.etterbetaling.length > 0 && <UtbetalingTable tittel="Etterbetaling" perioder={data.etterbetaling} />}
        {data.tilbakekreving.length > 0 && <UtbetalingTable tittel="Tilbakekreving" perioder={data.tilbakekreving} />}
      </VStack>
    )
  }

  return (
    <>
      {oppsummeringer.perAar.map(({ aarstall, oppsummering }) => {
        const etterbetaling = hentPerioderForAar(aarstall, data.etterbetaling)
        const tilbakekreving = hentPerioderForAar(aarstall, data.tilbakekreving)

        return (
          <Box key={aarstall} maxWidth="70rem" background="neutral-soft" padding="space-20">
            <Heading level="3" size="small">
              Resultat av simulering i {aarstall}
            </Heading>
            <Box width="25rem" marginBlock="space-20">
              <VStack gap="space-20">
                <SumTabell>
                  <EtterbetalingRader etterbetaling={oppsummering.etterbetaling} />
                </SumTabell>
                {oppsummering.feilutbetaling && (
                  <SumTabell>
                    <FeilutbetalingRader feilutbetaling={oppsummering.feilutbetaling} />
                  </SumTabell>
                )}
              </VStack>
            </Box>
            <Box maxWidth="1000px">
              <ReadMore header={`Se detaljer om simulering i ${aarstall}`}>
                <VStack gap="space-20">
                  <UtbetalingTable tittel={`Etterbetaling ${aarstall}`} perioder={etterbetaling} />
                  <UtbetalingTable tittel={`Tilbakekreving ${aarstall}`} perioder={tilbakekreving} />
                </VStack>
              </ReadMore>
            </Box>
          </Box>
        )
      })}
      {oppsummeringer.perAar.length > 1 && (
        <Box maxWidth="70rem" background="neutral-soft" padding="space-20">
          <Heading level="3" size="small">
            Etterbetaling for hele perioden
          </Heading>
          <Box width="25rem" marginBlock="space-20">
            <VStack gap="space-20">
              <SumTabell>
                <EtterbetalingRader etterbetaling={oppsummeringer.forPerioden.etterbetaling} suffix=" for perioden" />
              </SumTabell>
              {oppsummeringer.forPerioden.feilutbetaling && (
                <SumTabell>
                  <FeilutbetalingRader
                    feilutbetaling={oppsummeringer.forPerioden.feilutbetaling}
                    suffix=" for perioden"
                  />
                </SumTabell>
              )}
            </VStack>
          </Box>
        </Box>
      )}
    </>
  )
}

function hentPerioderForAar(aarstall: number, perioder: SimulertBeregningsperiode[]) {
  return perioder.filter((periode) => {
    const aar = getYear(periode.fom)
    return aar === aarstall
  })
}
