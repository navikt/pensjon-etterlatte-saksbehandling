import styled from 'styled-components'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
  formaterStringTidspunktTimeMinutter,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Alert, BodyShort, Heading, Tag } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveForBehandlingUnderBehandlingIkkeattestert, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'

import { isInitial, isPending, mapApiResult } from '~shared/api/apiUtils'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const [saksbehandlerPaaOppgave, setSaksbehandlerPaaOppgave] = useState<OppgaveSaksbehandler | null>(null)
  const [oppgaveForBehandlingStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestert
  )
  useEffect(() => {
    requesthentOppgaveForBehandling(
      { referanse: behandlingsInfo.behandlingId, sakId: behandlingsInfo.sakId },
      (saksbehandler) => {
        setSaksbehandlerPaaOppgave(saksbehandler)
      }
    )
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

  const fattetDato = behandlingsInfo.datoFattet
    ? formaterStringDato(behandlingsInfo.datoFattet) +
      ' kl. ' +
      formaterStringTidspunktTimeMinutter(behandlingsInfo.datoFattet)
    : null

  if (isInitial(oppgaveForBehandlingStatus) || isPending(oppgaveForBehandlingStatus)) {
    return <Spinner visible={true} label="Henter saksbehandler" />
  }

  return (
    <SidebarPanel border>
      <Heading size="small">{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
      <Heading size="xsmall" spacing>
        {hentStatus()}
      </Heading>
      {fattetDato && <Tekst>{fattetDato}</Tekst>}
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
      <div className="info">
        <Info>Saksbehandler</Info>
        {mapApiResult(
          oppgaveForBehandlingStatus,
          <Spinner visible={true} label="Henter saksbehandler" />,
          () => (
            <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
          ),
          () => (
            <>
              {saksbehandlerPaaOppgave ? (
                <Tekst>
                  {saksbehandlerPaaOppgave.saksbehandlerNavn
                    ? saksbehandlerPaaOppgave.saksbehandlerNavn
                    : saksbehandlerPaaOppgave.saksbehandlerIdent}
                </Tekst>
              ) : (
                <Alert variant="warning">Ingen saksbehandler har tatt denne oppgaven</Alert>
              )}
            </>
          )
        )}
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
      <SakFlexbox>
        <InfoSakId>Sakid: </InfoSakId>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </SakFlexbox>
    </SidebarPanel>
  )
}

const SakFlexbox = styled.div`
  display: flex;
  flex-direction: row;
  margin-top: 1em;
`
const InfoSakId = styled.div`
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
`

const Info = styled.div`
  font-size: 14px;
  font-weight: 600;
`

const Tekst = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: #595959;
`
