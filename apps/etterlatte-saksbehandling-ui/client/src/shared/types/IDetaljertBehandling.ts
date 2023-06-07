import { IHendelse } from '~shared/types/IHendelse'
import { JaNei } from '~shared/types/ISvar'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { IFamilieforhold, IPdlPerson } from '~shared/types/Person'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

export interface IDetaljertBehandling {
  id: string
  sak: number
  sakType: ISaksType
  gyldighetsprøving?: IGyldighetResultat
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  saksbehandlerId?: string
  datoFattet?: string //kommer som Instant fra backend
  datoAttestert?: string //kommer som Instant fra backend
  attestant?: string
  soeknadMottattDato: string
  virkningstidspunkt: Virkningstidspunkt | null
  utenlandstilsnitt: IUtenlandstilsnitt | undefined
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | undefined
  status: IBehandlingStatus
  hendelser: IHendelse[]
  familieforhold?: IFamilieforhold
  behandlingType: IBehandlingsType
  søker?: IPdlPerson
  prosesstype: IProsesstype
  revurderingsaarsak: Revurderingsaarsak | null
}

export enum IBehandlingsType {
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
}

export enum IBehandlingStatus {
  OPPRETTET = 'OPPRETTET',
  VILKAARSVURDERT = 'VILKAARSVURDERT',
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

export enum IProsesstype {
  MANUELL = 'MANUELL',
  AUTOMATISK = 'AUTOMATISK',
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
  AVDOEDES_FORUTGAAENDE_MEDLEMSKAP = 'AVDOEDES_FORUTGAAENDE_MEDLEMSKAP',
  BARNETS_MEDLEMSKAP = 'BARNETS_MEDLEMSKAP',
  SAMME_ADRESSE = 'GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE',
  BARN_BOR_PAA_AVDOEDES_ADRESSE = 'BARN_BOR_PAA_AVDOEDES_ADRESSE',
  BARN_INGEN_OPPGITT_UTLANDSADRESSE = 'BARN_INGEN_OPPGITT_UTLANDSADRESSE',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
  FORMAAL_FOR_YTELSEN = 'FORMAAL_FOR_YTELSEN',
  SAKEN_KAN_BEHANDLES_I_SYSTEMET = 'SAKEN_KAN_BEHANDLES_I_SYSTEMET',
}
