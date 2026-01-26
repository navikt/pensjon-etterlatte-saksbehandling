import {
  IBehandlingStatus,
  IBehandlingsType,
  IBoddEllerArbeidetUtlandet,
  UtlandstilknytningType,
  Vedtaksloesning,
} from '~shared/types/IDetaljertBehandling'
import { IHendelse } from '~shared/types/IHendelse'
import { SakType } from '~shared/types/sak'

export interface IBehandlingInfo {
  type: IBehandlingsType
  behandlingId: string
  sakId: number
  sakType: SakType
  sakEnhetId: string
  status: IBehandlingStatus
  kilde: Vedtaksloesning
  behandlendeSaksbehandler?: string
  attesterendeSaksbehandler?: string
  virkningsdato?: string
  datoFattet?: string
  datoAttestert?: string
  nasjonalEllerUtland: UtlandstilknytningType | null
  underkjentLogg?: IHendelse[]
  fattetLogg?: IHendelse[]
  attestertLogg?: IHendelse[]
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null
}

export enum BehandlingFane {
  DOKUMENTER = 'DOKUMENTER',
  SJEKKLISTE = 'SJEKKLISTE',
  HISTORIKK = 'HISTORIKK',
}
