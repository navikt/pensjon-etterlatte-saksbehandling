import styled from 'styled-components'
import { IBehandlingStatus, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterDatoMedKlokkeslett,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Alert, BodyShort, Heading, Tag } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { SidebarPanel } from '~shared/components/Sidebar'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveForReferanseUnderBehandling } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import { FlexRow } from '~shared/styled'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Tekst } from '~components/behandling/attestering/styled'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandlingsInfo.status,
    behandlingsInfo.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [oppgaveForBehandlingenStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForReferanseUnderBehandling
  )

  const hentOppgaveForBehandling = () => requesthentOppgaveForBehandling(behandlingsInfo.behandlingId)

  useEffect(() => {
    hentOppgaveForBehandling()
  }, [])

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
    <SidebarPanel border>
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

      <TagList>
        <li>
          <Tag variant={tagColors[behandlingsInfo.sakType]} size="small">
            {formaterSakstype(behandlingsInfo.sakType)}
          </Tag>
        </li>
        <li>
          {behandlingsInfo.nasjonalEllerUtland ? (
            <Tag variant={tagColors[behandlingsInfo.nasjonalEllerUtland]} size="small">
              {formaterEnumTilLesbarString(behandlingsInfo.nasjonalEllerUtland)}
            </Tag>
          ) : (
            <BodyShort>Du må velge en tilknytning</BodyShort>
          )}
        </li>
      </TagList>
      <div className="flex">
        <div className="info">
          <Info>Saksbehandler</Info>
          {mapApiResult(
            oppgaveForBehandlingenStatus,
            <Spinner visible={true} label="Henter oppgave" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
            ),
            (oppgave) =>
              !!oppgave?.saksbehandler ? (
                <Tekst>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Tekst>
              ) : (
                <Alert size="small" variant="warning">
                  Ingen saksbehandler har tatt denne oppgaven
                </Alert>
              )
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
          <Tekst>
            {behandlingsInfo.virkningsdato ? formaterStringDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}
          </Tekst>
        </div>
        <div>
          <Info>Vedtaksdato</Info>
          <Tekst>
            {behandlingsInfo.datoAttestert ? formaterStringDato(behandlingsInfo.datoAttestert) : 'Ikke satt'}
          </Tekst>
        </div>
      </div>
      {kommentarFraAttestant && (
        <div className="info">
          <Info>Kommentar fra attestant</Info>
          <Tekst>{kommentarFraAttestant}</Tekst>
        </div>
      )}
      <FlexRow align="center" $spacing={true}>
        <Info>Sakid:</Info>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </FlexRow>

      {mapSuccess(oppgaveForBehandlingenStatus, (oppgave) => (
        <SettPaaVent oppgave={oppgave} redigerbar={redigerbar} refreshOppgave={hentOppgaveForBehandling} />
      ))}
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
