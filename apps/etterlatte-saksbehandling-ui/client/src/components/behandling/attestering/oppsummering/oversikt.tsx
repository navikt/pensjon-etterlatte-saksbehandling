import styled from 'styled-components'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
  formaterStringTidspunkt,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Heading, Tag } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useEffect, useState } from 'react'
import { isFailure, isInitial, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveForBehandlingUnderBehandlingIkkeattestert } from '~shared/api/oppgaverny'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Oversikt = ({
  behandlingsInfo,
  children,
}: {
  behandlingsInfo: IBehandlingInfo
  children: JSX.Element
}) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const [saksbehandlerPaaOppgave, setSaksbehandlerPaaOppgave] = useState<string | null>(null)
  const [oppgaveForBehandlingStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestert
  )
  useEffect(() => {
    requesthentOppgaveForBehandling({ behandlingId: behandlingsInfo.behandlingId }, (saksbehandler, statusCode) => {
      if (statusCode === 200) {
        setSaksbehandlerPaaOppgave(saksbehandler)
      }
    })
  }, [])

  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.FATTET_VEDTAK:
        return 'Attestering'
      case IBehandlingStatus.IVERKSATT:
        return 'Iverksatt'
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
    ? formaterStringDato(behandlingsInfo.datoFattet) + ' kl. ' + formaterStringTidspunkt(behandlingsInfo.datoFattet)
    : null

  if (isInitial(oppgaveForBehandlingStatus) || isPending(oppgaveForBehandlingStatus)) {
    return <Spinner visible={true} label="Henter saksbehandler" />
  }

  return (
    <SidebarPanel>
      <Heading size={'small'}>{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
      <Heading size={'xsmall'} spacing>
        {hentStatus()}
      </Heading>
      {fattetDato && <Tekst>{fattetDato}</Tekst>}
      <TagList>
        <li>
          <Tag variant={tagColors[behandlingsInfo.sakType]} size={'small'}>
            {formaterSakstype(behandlingsInfo.sakType)}
          </Tag>
        </li>
        <li>
          <Tag variant={tagColors[INasjonalitetsType.NASJONAL]} size={'small'}>
            {formaterEnumTilLesbarString(INasjonalitetsType.NASJONAL)}
          </Tag>
        </li>
      </TagList>
      <div className="info">
        <Info>Saksbehandler</Info>
        {isFailure(oppgaveForBehandlingStatus) && (
          <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
        )}
        {isPendingOrInitial(oppgaveForBehandlingStatus) && <Spinner visible={true} label="Henter saksbehandler" />}
        {isSuccess(oppgaveForBehandlingStatus) && (
          <Tekst>
            {saksbehandlerPaaOppgave ? saksbehandlerPaaOppgave : 'Ingen saksbehandler har tatt denne oppgaven'}
          </Tekst>
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
      {children}
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
