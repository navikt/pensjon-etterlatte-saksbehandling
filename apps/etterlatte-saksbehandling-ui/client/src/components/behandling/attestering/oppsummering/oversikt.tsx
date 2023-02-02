import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterEnumTilLesbarString,
  formaterStringDato,
  formaterStringTidspunkt,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'
import { Tag } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.ident)

  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.FATTET_VEDTAK:
        return 'Attestering'
      case IBehandlingStatus.IVERKSATT:
        return 'Iverksatt'
      default:
        return 'Under behandling'
    }
  }

  const fattetDato = behandlingsInfo.datoFattet
    ? formaterStringDato(behandlingsInfo.datoFattet) + ' kl. ' + formaterStringTidspunkt(behandlingsInfo.datoFattet)
    : null

  return (
    <BehandlingsinfoContainer>
      <Overskrift>{formaterBehandlingstype(behandlingsInfo?.type)}</Overskrift>
      <UnderOverskrift>{hentStatus()}</UnderOverskrift>
      {fattetDato && <Tekst>{fattetDato}</Tekst>}
      <TagList>
        <li>
          <Tag variant={tagColors[ISaksType.BARNEPENSJON]} size={'small'}>
            {formaterEnumTilLesbarString(ISaksType.BARNEPENSJON)}
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
    </BehandlingsinfoContainer>
  )
}

const BehandlingsinfoContainer = styled.div`
  margin: 20px 8px 0px 8px;
  padding: 1em;
  border: 1px solid #c7c0c0;
  border-radius: 3px;

  .flex {
    display: flex;
    justify-content: space-between;
  }
  .info {
    margin-top: 1em;
    margin-bottom: 1em;
  }
`

const UnderOverskrift = styled.div`
  font-size: 16px;
  font-weight: 600;
  color: #005b82;
`

const Overskrift = styled.div`
  font-size: 20px;
  font-weight: 600;
  color: #3e3832;
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
