import styled from 'styled-components'
import { IBehandlingStatus, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { formaterDatoMedKlokkeslett, formaterDato } from '~utils/formatering/dato'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Alert, Box, Heading, HStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'
import React from 'react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { UtenlandstilknytningTypeTag } from '~shared/tags/UtenlandstilknytningTypeTag'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const oppgave = useSelectorOppgaveUnderBehandling()

  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.FATTET_VEDTAK:
        return 'Attestering'
      case IBehandlingStatus.TIL_SAMORDNING:
        return 'Til samordning'
      case IBehandlingStatus.SAMORDNET:
        return 'Samordnet'
      case IBehandlingStatus.IVERKSATT:
        return 'Iverksatt'
      case IBehandlingStatus.AVSLAG:
        return 'Avslag'
      case IBehandlingStatus.AVBRUTT:
        return 'Avbrutt'
      case IBehandlingStatus.OPPRETTET:
      case IBehandlingStatus.VILKAARSVURDERT:
      case IBehandlingStatus.BEREGNET:
      case IBehandlingStatus.ATTESTERT:
      case IBehandlingStatus.RETURNERT:
        return 'Under behandling'
    }
  }

  return (
    <SidebarPanel $border>
      <Heading size="small">
        {formaterBehandlingstype(behandlingsInfo.type)}

        {(behandlingsInfo.nasjonalEllerUtland === UtlandstilknytningType.UTLANDSTILSNITT ||
          behandlingsInfo.nasjonalEllerUtland === UtlandstilknytningType.BOSATT_UTLAND) && (
          <EessiPensjonLenke
            sakId={behandlingsInfo.sakId}
            behandlingId={behandlingsInfo.behandlingId}
            sakType={behandlingsInfo.sakType}
          />
        )}
      </Heading>

      <Heading size="xsmall" spacing>
        {hentStatus()}
      </Heading>

      {behandlingsInfo.datoFattet && <Tekst>{formaterDatoMedKlokkeslett(behandlingsInfo.datoFattet)}</Tekst>}

      <Box paddingInline="2 0">
        <HStack gap="2">
          <SakTypeTag sakType={behandlingsInfo.sakType} size="small" />
          <UtenlandstilknytningTypeTag utenlandstilknytningType={behandlingsInfo.nasjonalEllerUtland} size="small" />
        </HStack>
      </Box>

      <div className="flex">
        <div className="info">
          <Info>Saksbehandler</Info>
          {!!oppgave?.saksbehandler ? (
            <Tekst>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Tekst>
          ) : (
            <Alert size="small" variant="warning">
              Ingen saksbehandler har tatt denne oppgaven
            </Alert>
          )}
        </div>
        <div className="info">
          <Info>Kilde</Info>
          <Tekst>{behandlingsInfo.kilde}</Tekst>
        </div>
      </div>
      <div className="flex">
        <div>
          <Info>Virkningstidspunkt</Info>
          <Tekst>{behandlingsInfo.virkningsdato ? formaterDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}</Tekst>
        </div>
        <div>
          <Info>Vedtaksdato</Info>
          <Tekst>{behandlingsInfo.datoAttestert ? formaterDato(behandlingsInfo.datoAttestert) : 'Ikke satt'}</Tekst>
        </div>
      </div>
      {kommentarFraAttestant && (
        <div className="info">
          <Info>Kommentar fra attestant</Info>
          <Tekst>{kommentarFraAttestant}</Tekst>
        </div>
      )}
      <HStack gap="4" align="center">
        <Info>Sakid:</Info>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </HStack>

      <SettPaaVent oppgave={oppgave} />
    </SidebarPanel>
  )
}

const Info = styled.div`
  font-size: 14px;
  font-weight: 600;
`

const Tekst = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: #595959;
`
