import { IHendelse } from '~shared/types/IHendelse'
import { JaNei } from '~shared/types/ISvar'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { Persongalleri } from '~shared/types/Person'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { SakType } from '~shared/types/sak'
import { RevurderingMedBegrunnelse } from '~shared/types/RevurderingInfo'
import { Spraak } from '~shared/types/Brev'

export interface IDetaljertBehandling {
  id: string
  sakId: number
  sakType: SakType
  gyldighetsprøving?: IGyldighetResultat
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  soeknadMottattDato?: string
  virkningstidspunkt: Virkningstidspunkt | null
  utlandstilknytning: IUtlandstilknytning | null
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null
  status: IBehandlingStatus
  hendelser: IHendelse[]
  behandlingType: IBehandlingsType
  revurderingsaarsak: Revurderingaarsak | null
  revurderinginfo: RevurderingMedBegrunnelse | null
  begrunnelse: string | null
  kilde: Vedtaksloesning
}

export interface NyBehandlingRequest {
  sakType?: SakType
  persongalleri?: Persongalleri
  mottattDato?: string
  spraak?: Spraak
  kilde?: string
  pesysId?: number
  enhet?: String
  foreldreloes?: boolean
  ufoere?: boolean
  gradering?: string
}

export enum IBehandlingsType {
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
}

export enum IBehandlingStatus {
  OPPRETTET = 'OPPRETTET',
  VILKAARSVURDERT = 'VILKAARSVURDERT',
  TRYGDETID_OPPDATERT = 'TRYGDETID_OPPDATERT',
  BEREGNET = 'BEREGNET',
  AVKORTET = 'AVKORTET',
  FATTET_VEDTAK = 'FATTET_VEDTAK',
  ATTESTERT = 'ATTESTERT',
  RETURNERT = 'RETURNERT',
  TIL_SAMORDNING = 'TIL_SAMORDNING',
  SAMORDNET = 'SAMORDNET',
  IVERKSATT = 'IVERKSATT',
  AVBRUTT = 'AVBRUTT',
  AVSLAG = 'AVSLAG',
}

export enum UtlandstilknytningType {
  NASJONAL = 'NASJONAL',
  UTLANDSTILSNITT = 'UTLANDSTILSNITT',
  BOSATT_UTLAND = 'BOSATT_UTLAND',
}

export interface IUtlandstilknytning {
  type: UtlandstilknytningType
  kilde: KildeSaksbehandler
  begrunnelse: string
}

export interface IBoddEllerArbeidetUtlandet {
  boddEllerArbeidetUtlandet: boolean
  kilde: KildeSaksbehandler
  begrunnelse: string
  boddArbeidetIkkeEosEllerAvtaleland?: boolean
  boddArbeidetEosNordiskKonvensjon?: boolean
  boddArbeidetAvtaleland?: boolean
  vurdereAvoededsTrygdeavtale?: boolean
  skalSendeKravpakke?: boolean
}

export interface IGyldighetResultat {
  resultat: VurderingsResultat | undefined
  vurderinger: IGyldighetproving[]
  vurdertDato: string
}

export enum GyldigFramsattType {
  INNSENDER_ER_FORELDER = 'INNSENDER_ER_FORELDER',
  HAR_FORELDREANSVAR_FOR_BARNET = 'HAR_FORELDREANSVAR_FOR_BARNET',
  INGEN_ANNEN_VERGE_ENN_FORELDER = 'INGEN_ANNEN_VERGE_ENN_FORELDER',
  INNSENDER_ER_GJENLEVENDE = 'INNSENDER_ER_GJENLEVENDE',
  MANUELL_VURDERING = 'MANUELL_VURDERING',
}

export interface IGyldighetproving {
  navn: GyldigFramsattType
  resultat: VurderingsResultat
  basertPaaOpplysninger: IManuellVurdering | any
}
export interface IManuellVurdering {
  begrunnelse: string
  kilde: KildeSaksbehandler
}
export interface IKommerBarnetTilgode {
  svar: JaNei
  begrunnelse: string
  kilde: KildeSaksbehandler
}

export interface Virkningstidspunkt {
  dato: string
  kilde: KildeSaksbehandler
  begrunnelse: string
  kravdato: string | null
}

export enum Vedtaksloesning {
  GJENNY = 'GJENNY',
  PESYS = 'PESYS',
  GJENOPPRETTA = 'GJENOPPRETTA',
}
