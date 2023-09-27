import { IHendelse } from '~shared/types/IHendelse'
import { JaNei } from '~shared/types/ISvar'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { IFamilieforhold, IPdlPerson, Persongalleri } from '~shared/types/Person'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { SakType } from '~shared/types/sak'
import { RevurderingMedBegrunnelse } from '~shared/types/RevurderingInfo'

export interface IDetaljertBehandling {
  id: string
  sakId: number
  sakType: SakType
  gyldighetsprøving?: IGyldighetResultat
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  soeknadMottattDato: string
  virkningstidspunkt: Virkningstidspunkt | null
  utenlandstilsnitt: IUtenlandstilsnitt | undefined
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null
  status: IBehandlingStatus
  hendelser: IHendelse[]
  familieforhold?: IFamilieforhold
  behandlingType: IBehandlingsType
  søker?: IPdlPerson
  revurderingsaarsak: Revurderingsaarsak | null
  revurderinginfo: RevurderingMedBegrunnelse | null
  begrunnelse: string | null
  etterbetaling: IEtterbetaling | null
}

export interface NyBehandlingRequest {
  sakType?: SakType
  persongalleri?: Persongalleri
  mottattDato?: string
  spraak?: string
}

export enum IBehandlingsType {
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
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
  IVERKSATT = 'IVERKSATT',
  AVBRUTT = 'AVBRUTT',
}

export enum IUtenlandstilsnittType {
  NASJONAL = 'NASJONAL',
  UTLANDSTILSNITT = 'UTLANDSTILSNITT',
  BOSATT_UTLAND = 'BOSATT_UTLAND',
}

export interface IUtenlandstilsnitt {
  type: IUtenlandstilsnittType
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
  norgeErBehandlendeland?: boolean
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
}

export interface IEtterbetaling {
  fraDato?: Date | null
  tilDato?: Date | null
}
