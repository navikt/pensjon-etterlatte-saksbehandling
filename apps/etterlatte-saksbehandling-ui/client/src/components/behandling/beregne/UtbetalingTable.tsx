import { Box, Heading, Label, Table } from '@navikt/ds-react'
import { compareDesc } from 'date-fns'
import { SimulertBeregningsperiode } from '~shared/types/Utbetaling'
import { formaterDato, formaterKanskjeStringDato } from '~utils/formatering/dato'
import { NOK } from '~utils/formatering/formatering'

export function summerPerioder(perioder: SimulertBeregningsperiode[]) {
  return perioder.map((row) => row.beloep).reduce((sum, current) => sum + current, 0)
}

export const UtbetalingTable = ({ tittel, perioder }: { tittel: string; perioder: SimulertBeregningsperiode[] }) => {
  const sortertePerioder = [...perioder].sort((a, b) => compareDesc(new Date(a.fom), new Date(b.fom)))

  if (sortertePerioder.length > 0) {
    return (
      <Box maxWidth="70rem" marginBlock="space-16">
        <Heading level="3" size="xsmall">
          {tittel}
        </Heading>
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Klasse</Table.HeaderCell>
              <Table.HeaderCell>Konto</Table.HeaderCell>
              <Table.HeaderCell>Forfall</Table.HeaderCell>
              <Table.HeaderCell align="right">Beløp</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortertePerioder.map((periode, idx) => (
              <Table.Row key={idx}>
                <Table.DataCell>
                  {formaterDato(periode.fom)} - {formaterKanskjeStringDato(periode.tom)}
                </Table.DataCell>
                <Table.DataCell>
                  {periode.klassekodeBeskrivelse} {periode.tilbakefoering && '(tidligere utbetalt)'}
                </Table.DataCell>
                <Table.DataCell>{periode.konto}</Table.DataCell>
                <Table.DataCell>{formaterDato(periode.forfall)}</Table.DataCell>
                <Table.DataCell align="right">{NOK(periode.beloep)}</Table.DataCell>
              </Table.Row>
            ))}
            <Table.Row>
              <Table.DataCell colSpan={4}>
                <Label>Sum</Label>
              </Table.DataCell>
              <Table.DataCell align="right">{NOK(summerPerioder(perioder))}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </Box>
    )
  } else {
    return (
      <Box maxWidth="70rem">
        <Heading level="3" size="xsmall">
          {tittel}
        </Heading>
        <p>Ingen perioder</p>
      </Box>
    )
  }
}
