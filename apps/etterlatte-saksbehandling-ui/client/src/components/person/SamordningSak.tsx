import React, { useEffect, useState } from 'react'
import { Box, Heading, Link, Table } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSamordningsdataForSak } from '~shared/api/vedtaksvurdering'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterDato } from '~utils/formatering/dato'
import { SakMedBehandlinger } from '~components/person/typer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Samordningsvedtak } from '~components/vedtak/typer'
import SamordningOppdaterMeldingModal from '~components/person/SamordningOppdaterMeldingModal'

export const SamordningSak = ({ fnr, sakResult }: { fnr: string; sakResult: Result<SakMedBehandlinger> }) => {
  const [samordningdataStatus, hent] = useApiCall(hentSamordningsdataForSak)
  const [sakId, setSakId] = useState<number>()

  useEffect(() => {
    if (isSuccess(sakResult)) {
      setSakId(sakResult.data.sak.id)
      hent(sakResult.data.sak.id)
    }
  }, [sakResult])

  return (
    <Box padding="space-8">
      <Heading size="medium">Samordningsmeldinger</Heading>

      {mapResult(samordningdataStatus, {
        success: (data) => (
          <SamordningTabell fnr={fnr} sakId={sakId!} samordningsdata={data} refresh={() => hent(sakId!)} />
        ),
        pending: <Spinner label="Henter samordningsdata" />,
        error: () => <ApiErrorAlert>Kunne ikke hente samordningsdata</ApiErrorAlert>,
      })}
    </Box>
  )
}

function SamordningTabell({
  fnr,
  sakId,
  samordningsdata,
  refresh,
}: {
  fnr: string
  sakId: number
  samordningsdata: Array<Samordningsvedtak>
  refresh: () => void
}) {
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
          <Table.HeaderCell>Overstyr</Table.HeaderCell>
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
                <Table.DataCell>{formaterDato(mld.sendtDato)}</Table.DataCell>
                <Table.DataCell>{mld.svartDato && formaterDato(mld.svartDato)}</Table.DataCell>
                <Table.DataCell>{mld.purretDato && formaterDato(mld.purretDato)}</Table.DataCell>
                <Table.DataCell>{mld.svartDato && (mld.refusjonskrav ? 'Ja' : 'Nei')}</Table.DataCell>
                <Table.DataCell>
                  {!mld.svartDato && (
                    <SamordningOppdaterMeldingModal
                      fnr={fnr}
                      sakId={sakId}
                      mld={mld}
                      vedtakId={vedtak.vedtakId}
                      refresh={refresh}
                    />
                  )}
                </Table.DataCell>
              </Table.Row>
            ))
          )}
      </Table.Body>
    </Table>
  )
}
