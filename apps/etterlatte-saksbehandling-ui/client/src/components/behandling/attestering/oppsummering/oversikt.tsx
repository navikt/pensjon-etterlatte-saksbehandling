import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
  formaterStringTidspunkt,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'
import { Heading, Tag } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'

export const Oversikt = ({
  behandlingsInfo,
  children,
}: {
  behandlingsInfo: IBehandlingInfo
  children: JSX.Element
}) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.ident)
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar

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
        <Tekst>{behandlingsInfo.saksbehandler ? behandlingsInfo.saksbehandler : innloggetSaksbehandler}</Tekst>
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
