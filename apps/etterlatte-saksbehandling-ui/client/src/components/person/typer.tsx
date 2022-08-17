import { IBehandlingStatus } from "../../store/reducers/BehandlingReducer";
import { IBehandlingsType } from "../behandling/behandlingsType";

export interface Behandling {
  id: number
  opprettet: string
  type: string
  årsak: string
  status: string
  vedtaksdato: string
  resultat: string
}


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
  id: string,
  sak: number,
  status: IBehandlingStatus,
  soeknadMottattDato: string,
  behandlingOpprettet: string,
  behandlingType: IBehandlingsType
}
