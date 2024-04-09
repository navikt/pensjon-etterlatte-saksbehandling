import { Heading, Link, Table } from '@navikt/ds-react'
import { IBehandlingsammendrag, SakMedBehandlinger } from '../typer'
import { formaterBehandlingstype, formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
import React, { useEffect } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { hentGenerelleBehandlingForSak } from '~shared/api/generellbehandling'
import { Generellbehandling } from '~shared/types/Generellbehandling'
import {
  behandlingStatusTilLesbartnavn,
  genbehandlingTypeTilLesbartNavn,
  generellBehandlingsStatusTilLesbartNavn,
  lenkeTilBehandling,
  mapAarsak,
} from '~components/person/behandlingsslistemappere'
import { VedtakKolonner } from '~components/person/VedtakKoloner'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'

const BehandlingstypeWrapper = styled.div`
  display: flex;
  flex-direction: row;
  gap: 0.5em;
`

type alleBehandlingsTyper = IBehandlingsammendrag | Generellbehandling

function erVanligBehandling(behandling: alleBehandlingsTyper): behandling is IBehandlingsammendrag {
  return (behandling as IBehandlingsammendrag).soeknadMottattDato !== undefined
}

function hentDato(behandling: alleBehandlingsTyper): string {
  if (erVanligBehandling(behandling)) {
    return behandling.behandlingOpprettet
  } else {
    return behandling.opprettet
  }
}

export const BehandlingTable = ({ sakOgBehandlinger }: { sakOgBehandlinger: SakMedBehandlinger }) => {
  const [generellbehandlingStatus, hentGenerellbehandlinger] = useApiCall(hentGenerelleBehandlingForSak)

  const { sak, behandlinger } = sakOgBehandlinger

  useEffect(() => {
    hentGenerellbehandlinger(sak.id)
  }, [sak.id])

  let allebehandlinger: alleBehandlingsTyper[] = []
  allebehandlinger = allebehandlinger.concat(behandlinger)
  if (isSuccess(generellbehandlingStatus)) {
    allebehandlinger = allebehandlinger.concat(generellbehandlingStatus.data)
  }

  allebehandlinger.sort((a, b) => (new Date(hentDato(a)) < new Date(hentDato(b)) ? 1 : -1))

  return (
    <>
      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Reg. dato</Table.HeaderCell>
            <Table.HeaderCell>Behandlingstype</Table.HeaderCell>
            <Table.HeaderCell>Årsak</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>Virkningstidspunkt</Table.HeaderCell>
            <Table.HeaderCell>Vedtaksdato</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {allebehandlinger.map((behandling, i) => {
            if (erVanligBehandling(behandling)) {
              return (
                <Table.Row key={i}>
                  <Table.DataCell>{formaterStringDato(behandling.behandlingOpprettet)}</Table.DataCell>
                  <Table.DataCell>
                    <BehandlingstypeWrapper>
                      {formaterBehandlingstype(behandling.behandlingType)}
                      {(sak.utlandstilknytning?.type === UtlandstilknytningType.UTLANDSTILSNITT ||
                        sak.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND) && (
                        <EessiPensjonLenke sakId={sak.id} behandlingId={behandling.id} sakType={sak.sakType} />
                      )}
                    </BehandlingstypeWrapper>
                  </Table.DataCell>
                  <Table.DataCell>{mapAarsak(behandling.aarsak)}</Table.DataCell>
                  <Table.DataCell>
                    {formaterEnumTilLesbarString(behandlingStatusTilLesbartnavn(behandling.status))}
                  </Table.DataCell>
                  <Table.DataCell>
                    {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt!!.dato) : ''}
                  </Table.DataCell>
                  <VedtakKolonner behandlingId={behandling.id} />
                  <Table.DataCell>
                    <Link href={lenkeTilBehandling(behandling)}>Gå til behandling</Link>
                  </Table.DataCell>
                </Table.Row>
              )
            } else {
              return (
                <Table.Row key={i}>
                  <Table.DataCell>{formaterStringDato(behandling.opprettet)}</Table.DataCell>
                  <Table.DataCell>
                    <BehandlingstypeWrapper>
                      {genbehandlingTypeTilLesbartNavn(behandling.type)}
                      <EessiPensjonLenke sakId={sak.id} behandlingId={behandling.id} sakType={sak.sakType} />
                    </BehandlingstypeWrapper>
                  </Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                  <Table.DataCell>{generellBehandlingsStatusTilLesbartNavn(behandling.status)}</Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                  <Table.DataCell>
                    <Link href={`/generellbehandling/${behandling.id}`}>Gå til behandling</Link>
                  </Table.DataCell>
                </Table.Row>
              )
            }
          })}
        </Table.Body>
      </Table>
      {isPending(generellbehandlingStatus) && <Spinner visible={true} label="Henter generelle behandlinger" />}
      {isFailureHandler({
        apiResult: generellbehandlingStatus,
        errorMessage: 'Vi klarte ikke å hente generelle behandligner',
      })}
    </>
  )
}
