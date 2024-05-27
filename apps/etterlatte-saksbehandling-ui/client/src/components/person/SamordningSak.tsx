import React, { useEffect } from 'react'
import { Box, Heading, Link, Table } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSamordningsdataForSak } from '~shared/api/vedtaksvurdering'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterStringDato } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Samordningsvedtak } from '~components/vedtak/typer'

export const SamordningSak = ({ sakResult }: { sakResult: Result<SakMedBehandlinger> }) => {
  const [samordningdataStatus, hent] = useApiCall(hentSamordningsdataForSak)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hent(sakResult.data.sak.id)
    }
  }, [sakResult])

  return (
    <Box padding="8">
      <Heading size="medium">Samordningsmeldinger</Heading>

      {mapResult(samordningdataStatus, {
        success: (data) => <SamordningTabell samordningsdata={data} />,
        pending: <Spinner visible={true} label="Henter samordningsdata" />,
        error: () => <ApiErrorAlert>Kunne ikke hente samordningsdata</ApiErrorAlert>,
      })}
    </Box>
  )
}

function SamordningTabell({ samordningsdata }: { samordningsdata: Array<Samordningsvedtak> }) {
  return samordningsdata.length == 0 ? (
    <p>Ingen samordningsmeldinger</p>
  ) : (
    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>VedtaksID</Table.HeaderCell>
          <Table.HeaderCell>Virkningstidspunkt</Table.HeaderCell>
          <Table.HeaderCell>SamordningsID</Table.HeaderCell>
          <Table.HeaderCell>Tjenestepensjon</Table.HeaderCell>
          <Table.HeaderCell>Status</Table.HeaderCell>
          <Table.HeaderCell>Sendt dato</Table.HeaderCell>
          <Table.HeaderCell>Mottatt dato</Table.HeaderCell>
          <Table.HeaderCell>Purret dato</Table.HeaderCell>
          <Table.HeaderCell>Refusjonskrav</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {samordningsdata
          .sort((v1, v2) => v2.vedtakId - v1.vedtakId)
          .map((vedtak) =>
            vedtak.samordningsmeldinger.map((mld) => (
              <Table.Row key={mld.samId}>
                <Table.DataCell>
                  <Link href={`/behandling/${vedtak.behandlingId}`}>{vedtak.vedtakId}</Link>
                </Table.DataCell>
                <Table.DataCell>{vedtak.virkningFom}</Table.DataCell>
                <Table.DataCell>{mld.samId}</Table.DataCell>
                <Table.DataCell>
                  {mld.tpNr} {mld.tpNavn}
                </Table.DataCell>
                <Table.DataCell>{mld.meldingstatusKode}</Table.DataCell>
                <Table.DataCell>{formaterStringDato(mld.sendtDato)}</Table.DataCell>
                <Table.DataCell>{mld.svartDato && formaterStringDato(mld.svartDato)}</Table.DataCell>
                <Table.DataCell>{mld.purretDato && formaterStringDato(mld.purretDato)}</Table.DataCell>
                <Table.DataCell>{mld.refusjonskrav}</Table.DataCell>
              </Table.Row>
            ))
          )}
      </Table.Body>
    </Table>
  )
}
