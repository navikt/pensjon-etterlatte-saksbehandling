import { Heading, Link, Table, Tag } from '@navikt/ds-react'
import { AarsaksTyper, BehandlingOgRevurderingsAarsakerType, IBehandlingsammendrag } from './typer'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterStringDato,
  formaterVedtakType,
} from '~utils/formattering'
import { IBehandlingStatus, IBehandlingsType, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import React, { useEffect } from 'react'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { tagColors } from '~shared/Tags'
import styled from 'styled-components'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { ExclamationmarkTriangleFillIcon } from '@navikt/aksel-icons'

const VedtakKolonner = (props: { behandlingId: string }) => {
  const [vedtak, apiHentVedtaksammendrag] = useApiCall(hentVedtakSammendrag)

  useEffect(() => {
    apiHentVedtaksammendrag(props.behandlingId)
  }, [])

  const attestertDato = (dato?: string) => {
    if (dato) return formaterStringDato(dato)
    else return ''
  }

  return (
    <>
      {isPending(vedtak) && (
        <Table.DataCell>
          <Spinner visible label="" margin="0" />
        </Table.DataCell>
      )}
      {isSuccess(vedtak) && (
        <>
          <Table.DataCell>{attestertDato(vedtak.data?.datoAttestert)}</Table.DataCell>
          <Table.DataCell>{vedtak.data?.vedtakType && formaterVedtakType(vedtak.data.vedtakType)}</Table.DataCell>
        </>
      )}
      {isFailure(vedtak) && (
        <ExclamationmarkTriangleFillIcon
          title={vedtak.error.detail || 'Feil oppsto ved henting av sammendrag for behandling'}
        />
      )}
    </>
  )
}

const lenkeTilBehandling = (behandlingSammendrag: IBehandlingsammendrag): string => {
  switch (behandlingSammendrag.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return `/behandling/${behandlingSammendrag.id}/soeknadsoversikt`
    case IBehandlingsType.REVURDERING:
      return `/behandling/${behandlingSammendrag.id}/revurderingsoversikt`
    case IBehandlingsType.MANUELT_OPPHOER:
      return `/behandling/${behandlingSammendrag.id}/opphoeroversikt`
  }
}

export const Behandlingsliste = ({ behandlinger }: { behandlinger: IBehandlingsammendrag[] }) => {
  const sorterteBehandlinger = behandlinger.sort((a, b) =>
    new Date(b.behandlingOpprettet!) > new Date(a.behandlingOpprettet!) ? 1 : -1
  )

  /**
   * TODO:
   *  Burde ha en form for paginering av behandlinger. Visning av tabellen og henting av vedtakssammendrag henger
   *  seg opp når det er for mange elementer i listen. Eksempelvis testbruker STOR SNERK som har 210 behandlinger i
   *  skrivende stund.
   */
  return (
    <BehandlingPanel>
      <Heading size="medium">Behandlinger</Heading>

      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Reg. dato</Table.HeaderCell>
            <Table.HeaderCell>Behandlingstype</Table.HeaderCell>
            <Table.HeaderCell>Utenlandstilsnitt</Table.HeaderCell>
            <Table.HeaderCell>Årsak</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>Virkningstidspunkt</Table.HeaderCell>
            <Table.HeaderCell>Vedtaksdato</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {sorterteBehandlinger.map((behandling, i) => (
            <Table.Row key={i} shadeOnHover={false}>
              <Table.DataCell>{formaterStringDato(behandling.behandlingOpprettet)}</Table.DataCell>
              <Table.DataCell>
                <BehandlingstypeWrapper>{formaterBehandlingstype(behandling.behandlingType)}</BehandlingstypeWrapper>
              </Table.DataCell>
              <Table.DataCell>
                <Tag
                  variant={tagColors[behandling.utenlandstilsnitt?.type || UtenlandstilknytningType.NASJONAL]}
                  size="small"
                >
                  {formaterEnumTilLesbarString(behandling.utenlandstilsnitt?.type || UtenlandstilknytningType.NASJONAL)}
                </Tag>
              </Table.DataCell>
              <Table.DataCell>{mapAarsak(behandling.aarsak)}</Table.DataCell>
              <Table.DataCell>{formaterEnumTilLesbarString(endringStatusNavn(behandling.status))}</Table.DataCell>
              <Table.DataCell>
                {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt!!.dato) : ''}
              </Table.DataCell>
              <VedtakKolonner behandlingId={behandling.id} />
              <Table.DataCell>
                <Link href={lenkeTilBehandling(behandling)}>Vis behandling</Link>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </BehandlingPanel>
  )
}

function mapAarsak(aarsak: BehandlingOgRevurderingsAarsakerType) {
  switch (aarsak) {
    case AarsaksTyper.MANUELT_OPPHOER:
      return 'Manuelt opphør'
    case AarsaksTyper.SOEKNAD:
      return 'Søknad'
    case AarsaksTyper.REVURDERING:
      return 'Revurdering'
    case Revurderingaarsak.REGULERING:
      return 'Regulering'
    case Revurderingaarsak.ANSVARLIGE_FORELDRE:
      return 'Ansvarlige foreldre'
    case Revurderingaarsak.SOESKENJUSTERING:
      return 'Søskenjustering'
    case Revurderingaarsak.UTLAND:
      return 'Ut-/innflytting til Norge'
    case Revurderingaarsak.BARN:
      return 'Barn'
    case Revurderingaarsak.DOEDSFALL:
      return 'Dødsfall'
    case Revurderingaarsak.OMGJOERING_AV_FARSKAP:
      return 'Omgjøring av farskap'
    case Revurderingaarsak.ADOPSJON:
      return 'Adopsjon'
    case Revurderingaarsak.VERGEMAAL_ELLER_FREMTIDSFULLMAKT:
      return 'Institusjonsopphold'
    case Revurderingaarsak.SIVILSTAND:
      return 'Endring av sivilstand'
    case Revurderingaarsak.NY_SOEKNAD:
      return 'Ny Søknad'
    case Revurderingaarsak.FENGSELSOPPHOLD:
      return 'Fengselsopphold'
    case Revurderingaarsak.YRKESSKADE:
      return 'Yrkesskade'
    case Revurderingaarsak.UT_AV_FENGSEL:
      return 'Ut av fengsel'
    case Revurderingaarsak.ANNEN:
      return 'Annen'
  }
}

function endringStatusNavn(status: IBehandlingStatus) {
  switch (status) {
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Til attestering'
    case IBehandlingStatus.ATTESTERT:
      return 'Attestert'
    case IBehandlingStatus.TIL_SAMORDNING:
      return 'Til samordning'
    case IBehandlingStatus.SAMORDNET:
      return 'Samordnet'
    case IBehandlingStatus.IVERKSATT:
      return 'Iverksatt'
    default:
      return status
  }
}

const BehandlingPanel = styled.div`
  margin: 3rem 0;
`

const BehandlingstypeWrapper = styled.div`
  display: flex;
  flex-direction: row;
  gap: 0.5em;
`
