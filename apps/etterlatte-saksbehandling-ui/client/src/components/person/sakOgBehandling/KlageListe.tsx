import {
  KabalResultat,
  KabalStatus,
  Klage,
  KlageStatus,
  teksterKabalstatus,
  teksterKabalUtfall,
  teksterKlagestatus,
  teksterKlageutfall,
} from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentKlagerISak } from '~shared/api/klage'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Alert, Link, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { JaNei } from '~shared/types/ISvar'

import { mapApiResult } from '~shared/api/apiUtils'

function formaterKlagestatus(status: KlageStatus): string | null {
  return teksterKlagestatus[status]
}

function formaterKlageResultat(klage: Klage): string | null {
  const formkravOppfylt = klage.formkrav?.formkrav?.erFormkraveneOppfylt
  if (formkravOppfylt === JaNei.JA) {
    const utfall = klage.utfall?.utfall
    if (utfall !== undefined) {
      return teksterKlageutfall[utfall]
    } else {
      return 'Utfall ikke vurdert'
    }
  } else if (formkravOppfylt === JaNei.NEI) {
    return 'Formkrav ikke oppfylt'
  } else {
    return 'Formkrav ikke avklart'
  }
}

function formaterKabalstatus(kabalStatus: KabalStatus | undefined) {
  if (kabalStatus === undefined) {
    return 'Ikke sendt til kabal'
  }
  return teksterKabalstatus[kabalStatus]
}

export function lenkeTilKlageMedId(id: string): string {
  return `/klage/${id}/`
}

function formaterKabalUtfall(kabalResultat: KabalResultat | undefined): string {
  if (kabalResultat === undefined) {
    return '-'
  }
  return teksterKabalUtfall[kabalResultat]
}

function KlageTabell(props: { klager: Array<Klage> }) {
  const { klager } = props

  return !!klager?.length ? (
    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Reg. dato</Table.HeaderCell>
          <Table.HeaderCell>Status</Table.HeaderCell>
          <Table.HeaderCell>Resultat Gjenny</Table.HeaderCell>
          <Table.HeaderCell>Status Kabal</Table.HeaderCell>
          <Table.HeaderCell>Resultat Kabal</Table.HeaderCell>
          <Table.HeaderCell>Handling</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {klager.map((klage) => (
          <Table.Row key={klage.id}>
            <Table.DataCell>{formaterDato(klage.opprettet)}</Table.DataCell>
            <Table.DataCell>{formaterKlagestatus(klage.status)}</Table.DataCell>
            <Table.DataCell>{formaterKlageResultat(klage)}</Table.DataCell>
            <Table.DataCell>{formaterKabalstatus(klage.kabalStatus)}</Table.DataCell>
            <Table.DataCell>{formaterKabalUtfall(klage.kabalResultat)}</Table.DataCell>
            <Table.DataCell>
              <Link href={lenkeTilKlageMedId(klage.id)}>Vis behandling</Link>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  ) : (
    <Alert variant="info" inline>
      Ingen klager på sak
    </Alert>
  )
}

export function KlageListe(props: { sakId: number }) {
  const { sakId } = props
  const [klager, hentKlager] = useApiCall(hentKlagerISak)

  useEffect(() => {
    void hentKlager(sakId)
  }, [sakId])

  return mapApiResult(
    klager,
    <Spinner label="Henter klager i saken" />,
    () => <ApiErrorAlert>Kunne ikke hente klager</ApiErrorAlert>,
    (klageliste) => {
      klageliste.sort((a, b) => (new Date(a.opprettet) < new Date(b.opprettet) ? 1 : -1))
      return <KlageTabell klager={klageliste} />
    }
  )
}
