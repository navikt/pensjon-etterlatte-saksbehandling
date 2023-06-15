import { Link, Table, Tag } from '@navikt/ds-react'
import { AarsaksTyper, BehandlingOgRevurderingsAarsakerType, IBehandlingsammendrag, VedtakSammendrag } from './typer'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
} from '~utils/formattering'
import { IBehandlingStatus, IBehandlingsType, IUtenlandstilsnittType } from '~shared/types/IDetaljertBehandling'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import React, { useEffect, useState } from 'react'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { tagColors } from '~shared/Tags'
import styled from 'styled-components'

const kolonner = [
  'Reg. dato',
  'Sakid',
  'Sakstype',
  'Behandlingstype',
  'Årsak',
  'Status',
  'Virkningstidspunkt',
  'Vedtaksdato',
  'Resultat',
  '',
]

const VedtaksDato = (props: { behandlingsId: string }) => {
  const [vedtak, setVedtak] = useState<VedtakSammendrag | undefined>(undefined)

  useEffect(() => {
    const getVedtakSammendrag = async (behandlingsId: string) => {
      const response = await hentVedtakSammendrag(behandlingsId)

      if (response.status === 'ok') {
        const responseData: VedtakSammendrag = response?.data

        setVedtak(responseData)
      } else {
        setVedtak(response?.error)
      }
    }

    getVedtakSammendrag(props.behandlingsId)
  }, [])

  const attestertDato = vedtak?.datoAttestert

  return <>{attestertDato ? formaterStringDato(attestertDato) : ''}</>
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
  return (
    <>
      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            {kolonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {behandlinger.map((behandling, i) => (
            <Table.Row key={i} shadeOnHover={false}>
              <Table.DataCell key={`data${behandling.behandlingOpprettet}`}>
                {formaterStringDato(behandling.behandlingOpprettet)}
              </Table.DataCell>
              <Table.DataCell key={`data${behandling.sak}`}>{behandling.sak}</Table.DataCell>
              <Table.DataCell key={`data${behandling.sakType}`}>{formaterSakstype(behandling.sakType)}</Table.DataCell>
              <Table.DataCell key={`data${behandling.behandlingType}`}>
                <BehandlingstypeWrapper>
                  {formaterBehandlingstype(behandling.behandlingType)}
                  <Tag
                    variant={tagColors[behandling.utenlandstilsnitt?.type || IUtenlandstilsnittType.NASJONAL]}
                    size={'small'}
                  >
                    {formaterEnumTilLesbarString(behandling.utenlandstilsnitt?.type || IUtenlandstilsnittType.NASJONAL)}
                  </Tag>
                </BehandlingstypeWrapper>
              </Table.DataCell>
              <Table.DataCell key={`data${behandling.aarsak}`}>{mapAarsak(behandling.aarsak)}</Table.DataCell>
              <Table.DataCell key={`data${behandling.status}`}>
                {formaterEnumTilLesbarString(endringStatusNavn(behandling.status))}
              </Table.DataCell>
              <Table.DataCell key={'virkningstidspunkt'}>
                {behandling.virkningstidspunkt ? formaterStringDato(behandling.virkningstidspunkt!!.dato) : ''}
              </Table.DataCell>
              <Table.DataCell key={'vedtaksdato'}>
                <VedtaksDato behandlingsId={behandling.id} />
              </Table.DataCell>
              <Table.DataCell key={'resultat'}>
                {erFerdigBehandlet(behandling.status) ? resultatTekst(behandling) : ''}
              </Table.DataCell>
              <Table.DataCell key={i}>
                <Link href={lenkeTilBehandling(behandling)}>Vis behandling</Link>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </>
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
    case Revurderingsaarsak.REGULERING:
      return 'Regulering'
    case Revurderingsaarsak.ANSVARLIGE_FORELDRE:
      return 'Ansvarlige foreldre'
    case Revurderingsaarsak.SOESKENJUSTERING:
      return 'Søskenjustering'
    case Revurderingsaarsak.UTLAND:
      return 'Ut-/innflytting til Norge'
    case Revurderingsaarsak.BARN:
      return 'Barn'
    case Revurderingsaarsak.DOEDSFALL:
      return 'Dødsfall'
    case Revurderingsaarsak.OMGJOERING_AV_FARSKAP:
      return 'Omgjøring av farskap'
    case Revurderingsaarsak.ADOPSJON:
      return 'Adopsjon'
    case Revurderingsaarsak.VERGEMAAL_ELLER_FREMTIDSFULLMAKT:
      return 'Institusjonsopphold'
  }
}

function endringStatusNavn(status: IBehandlingStatus) {
  switch (status) {
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Til attestering'
    case IBehandlingStatus.ATTESTERT:
      return 'Attestert'
    case IBehandlingStatus.IVERKSATT:
      return 'Iverksatt'
    default:
      return status
  }
}

function resultatTekst(behandling: IBehandlingsammendrag): string {
  switch (behandling.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return behandling.status === IBehandlingStatus.AVBRUTT
        ? 'Avbrutt'
        : resultatTekstFoerstegangsbehandling(behandling.vilkaarsvurderingUtfall)
    case IBehandlingsType.MANUELT_OPPHOER:
      return behandling.status === IBehandlingStatus.AVBRUTT ? 'Avbrutt' : 'Opphørt: Må behandles i Pesys'
    case IBehandlingsType.REVURDERING:
    default:
      return ''
  }
}

function resultatTekstFoerstegangsbehandling(vilkaarsvurderingResultat?: VilkaarsvurderingResultat): string {
  switch (vilkaarsvurderingResultat) {
    case undefined:
      return ''
    case VilkaarsvurderingResultat.OPPFYLT:
      return 'Innvilget'
    case VilkaarsvurderingResultat.IKKE_OPPFYLT:
      return 'Avslått'
  }
}

const BehandlingstypeWrapper = styled.div`
  display: flex;
  flex-direction: row;
  gap: 0.5em;
`
