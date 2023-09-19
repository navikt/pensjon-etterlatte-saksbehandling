import { KabalStatus, Klage, KlageStatus, teksterKlagestatus, teksterKlageutfall } from '~shared/types/Klage'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentKlagerISak } from '~shared/api/klage'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Heading, Link, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import styled from 'styled-components'
import { JaNei } from '~shared/types/ISvar'

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
    return '-'
  }
  return null
}

export function lenkeTilKlageMedId(id: string): string {
  return `/klage/${id}/`
}

function KlageTabell(props: { klager: Array<Klage> }) {
  const { klager } = props

  if (klager.length === 0) {
    return null
  }

  return (
    <KlageWrapper>
      <Heading size="medium">Klager</Heading>

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
            <Table.Row key={klage.id} shadeOnHover={false}>
              <Table.DataCell>{formaterStringDato(klage.opprettet)}</Table.DataCell>
              <Table.DataCell>{formaterKlagestatus(klage.status)}</Table.DataCell>
              <Table.DataCell>{formaterKlageResultat(klage)}</Table.DataCell>
              <Table.DataCell>{formaterKabalstatus(klage.kabalStatus)}</Table.DataCell>
              <Table.DataCell>Vi har ikke noe resultat fra Kabal enda</Table.DataCell>
              <Table.DataCell>
                <Link href={lenkeTilKlageMedId(klage.id)}>Vis behandling</Link>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </KlageWrapper>
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
    <Spinner visible label="Henter klager i saken" />,
    () => <ApiErrorAlert>Kunne ikke hente klager</ApiErrorAlert>,
    (klageliste) => <KlageTabell klager={klageliste} />
  )
}

const KlageWrapper = styled.div`
  margin: 3rem 0;
`
