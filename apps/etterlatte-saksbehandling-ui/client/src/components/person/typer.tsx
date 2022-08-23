import { IBehandlingStatus, IBehandlingsType } from '../../store/reducers/BehandlingReducer'

export interface Dokument {
  dato: string
  tittel: string
  link: string
  status: string
}

export interface Dokumenter {
  brev: Dokument[]
}

export interface IPersonInfo {
  navn: string
  fnr: string
  type: string
}

export interface IPersonResult {
  person: {
    fornavn: string
    etternavn: string
    foedselsnummer: string
  }
  behandlingListe: {
    behandlinger: IBehandlingsammendrag[]
  }
}

export interface IBehandlingsammendrag {
  id: string
  sak: number
  status: IBehandlingStatus
  soeknadMottattDato: string
  behandlingOpprettet: string
  behandlingType: IBehandlingsType
  aarsak: AarsaksTyper
}

export enum AarsaksTyper {
  SOEKER_DOD = 'SOEKER_DOD',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
  SOEKNAD = 'SOEKNAD',
}
