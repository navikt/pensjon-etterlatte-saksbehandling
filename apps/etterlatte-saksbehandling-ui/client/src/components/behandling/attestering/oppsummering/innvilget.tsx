import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { useVedtaksResultat, VedtakResultat } from '~components/behandling/useVedtaksResultat'
import { Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'
import React from 'react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

function innvilgelsestekst(vedtaksresultat: VedtakResultat | null): string {
  switch (vedtaksresultat) {
    case 'innvilget':
      return 'Innvilget'
    case 'opphoer':
      return 'Opphørt'
    case 'avslag':
      return 'Avslått'
    case 'endring':
      return 'Revurdert'
    case null:
      return ''
  }
}

function Resultat({ vedtaksresultat }: { vedtaksresultat: VedtakResultat | null }) {
  const erInnvilget = vedtaksresultat == 'innvilget' || vedtaksresultat == 'endring'

  return (
    <Heading size="xsmall" style={{ color: erInnvilget ? '#007C2E' : '#881d0c' }}>
      {innvilgelsestekst(vedtaksresultat)}
    </Heading>
  )
}

export const Innvilget = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const virkningsdato = behandlingsInfo.virkningsdato ? formaterDato(behandlingsInfo.virkningsdato) : '-'
  const vedtaksResultat = useVedtaksResultat()
  const attestertDato = behandlingsInfo.datoAttestert
    ? formaterDato(behandlingsInfo.datoAttestert)
    : formaterDato(new Date())

  return (
    <SidebarPanel $border style={{ borderLeft: '5px solid #007C2E' }}>
      <VStack gap="space-4">
        <div>
          <Heading size="small">{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
          <Resultat vedtaksresultat={vedtaksResultat} />
        </div>

        <HStack gap="space-4" justify="space-between">
          <VStack gap="space-4">
            <div>
              <Label size="small">Attestant</Label>
              <Detail>{behandlingsInfo.attesterendeSaksbehandler || '-'}</Detail>
            </div>
            <div>
              <Label size="small">Virkningsdato</Label>
              <Detail>{virkningsdato}</Detail>
            </div>
          </VStack>

          <VStack gap="space-4">
            <div>
              <Label size="small">Saksbehandler</Label>
              <Detail>{behandlingsInfo.behandlendeSaksbehandler}</Detail>
            </div>
            <div>
              <Label size="small">Vedtaksdato</Label>
              <Detail>{attestertDato}</Detail>
            </div>
          </VStack>
        </HStack>

        <HStack gap="space-4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
