import { Link, Table } from '@navikt/ds-react'
import { AarsaksTyper, IBehandlingsammendrag, VedtakSammendrag } from './typer'
import { formaterBehandlingstype, formaterEnumTilLesbarString, formaterStringDato } from '~utils/formattering'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import React, { useEffect, useState } from 'react'
import { hentVedtakSammendrag } from '~shared/api/vedtak'
import { OpprettNyBehandling } from '~components/person/OpprettNyBehandling'

const colonner = ['Reg. dato', 'Type', 'Årsak', 'Status', 'Virkningstidspunkt', 'Vedtaksdato', 'Resultat', '']

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

  // TODO - sjekk om det er attestert eller fattet dato

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

export const Behandlingsliste = ({
  behandlinger,
  sakId,
  revurderinger,
}: {
  behandlinger: IBehandlingsammendrag[]
  sakId: number
  revurderinger: Array<string>
}) => {
  return (
    <>
      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            {colonner.map((col) => (
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
              <Table.DataCell key={`data${behandling.behandlingType}`}>
                {formaterBehandlingstype(behandling.behandlingType)}
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
      <OpprettNyBehandling revurderinger={revurderinger} sakId={sakId} />
    </>
  )
}

function mapAarsak(aarsak: AarsaksTyper) {
  switch (aarsak) {
    case AarsaksTyper.MANUELT_OPPHOER:
      return 'Manuelt opphør'
    case AarsaksTyper.SOEKER_DOD:
      return 'Søker er død'
    case AarsaksTyper.SOEKNAD:
      return 'Søknad'
    case AarsaksTyper.REGULERING:
      return 'Regulering'
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
