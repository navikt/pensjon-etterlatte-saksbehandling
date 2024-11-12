import { KildeSaksbehandler } from '~shared/types/kilde'
import { JaNei } from '~shared/types/ISvar'
import { OppgaveDTO } from '~shared/types/oppgave'
import { ISak } from '~shared/types/sak'
import { IBrevAktivitetspliktDto } from '~shared/api/aktivitetsplikt'

export interface AktivitetspliktOppfolging {
  behandlingId: string
  aktivitet: string
  opprettet: string
  opprettetAv: string
}

export interface IAktivitet {
  id: string
  sakId: number
  behandlingId: string
  type: AktivitetspliktType
  fom: string
  tom?: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
}

export interface IOpprettAktivitet {
  id: string | undefined
  sakId: number
  type: AktivitetspliktType
  fom: string
  tom?: string
  beskrivelse: string
}

export enum AktivitetspliktType {
  ARBEIDSTAKER = 'ARBEIDSTAKER',
  SELVSTENDIG_NAERINGSDRIVENDE = 'SELVSTENDIG_NAERINGSDRIVENDE',
  ETABLERER_VIRKSOMHET = 'ETABLERER_VIRKSOMHET',
  ARBEIDSSOEKER = 'ARBEIDSSOEKER',
  UTDANNING = 'UTDANNING',
  INGEN_AKTIVITET = 'INGEN_AKTIVITET',
  OPPFOELGING_LOKALKONTOR = 'OPPFOELGING_LOKALKONTOR',
}

export enum AktivitetspliktVurderingType {
  AKTIVITET_UNDER_50 = 'AKTIVITET_UNDER_50',
  AKTIVITET_OVER_50 = 'AKTIVITET_OVER_50',
  AKTIVITET_100 = 'AKTIVITET_100',
}

//enum class Aktivitetsgrad { IKKE_I_AKTIVITET, UNDER_50_PROSENT, OVER_50_PROSENT, UNDER_100_PROSENT, AKKURAT_100_PROSENT }

export const tekstAktivitetspliktVurderingType: Record<AktivitetspliktVurderingType, string> = {
  AKTIVITET_UNDER_50: 'Under 50%',
  AKTIVITET_OVER_50: '50% - 99%',
  AKTIVITET_100: '100%',
}

export enum AktivitetspliktUnntakType {
  OMSORG_BARN_UNDER_ETT_AAR = 'OMSORG_BARN_UNDER_ETT_AAR',
  OMSORG_BARN_SYKDOM = 'OMSORG_BARN_SYKDOM',
  MANGLENDE_TILSYNSORDNING_SYKDOM = 'MANGLENDE_TILSYNSORDNING_SYKDOM',
  SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE = 'SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE',
  GRADERT_UFOERETRYGD = 'GRADERT_UFOERETRYGD',
  MIDLERTIDIG_SYKDOM = 'MIDLERTIDIG_SYKDOM',
  FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT = 'FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT',
}

export const tekstAktivitetspliktUnntakType: Record<AktivitetspliktUnntakType, string> = {
  OMSORG_BARN_UNDER_ETT_AAR: 'Omsorg for barn under ett år',
  OMSORG_BARN_SYKDOM: 'Omsorg for barn som har sykdom, skade eller funksjonshemming',
  MANGLENDE_TILSYNSORDNING_SYKDOM: 'Manglende tilsynsordning ved sykdom',
  SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE: 'Bruker har sykdom, redusert arbeidsevne, AAP',
  GRADERT_UFOERETRYGD: 'Gradert uføretrygd',
  MIDLERTIDIG_SYKDOM: 'Midlertidig sykdom',
  FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT: 'Nei, bruker er født i 1963 eller tidligere og har lav inntekt',
}

export interface IAktivitetspliktVurdering {
  aktivitet?: IAktivitetspliktAktivitetsgrad
  unntak?: IAktivitetspliktUnntak
}

export interface IAktivitetspliktVurderingNyDto {
  aktivitet: IAktivitetspliktAktivitetsgrad[]
  unntak: IAktivitetspliktUnntak[]
}

export function harVurdering(vurdering: IAktivitetspliktVurderingNyDto): boolean {
  return vurdering && !!(vurdering.aktivitet.length && vurdering.unntak.length)
}

export interface IAktivitetspliktAktivitetsgrad {
  id: string
  sakId: number
  behandlingId: string | undefined
  oppgaveId: string
  aktivitetsgrad: AktivitetspliktVurderingType
  fom: string
  tom: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
  skjoennsmessigVurdering?: AktivitetspliktSkjoennsmessigVurdering
  vurdertFra12Mnd?: boolean
}

export enum AktivitetspliktSkjoennsmessigVurdering {
  JA = 'JA',
  MED_OPPFOELGING = 'MED_OPPFOELGING',
  NEI = 'NEI',
}

export const teksterAktivitetspliktSkjoennsmessigVurdering: Record<AktivitetspliktSkjoennsmessigVurdering, string> = {
  JA: 'Ja',
  MED_OPPFOELGING: 'Med oppfølging',
  NEI: 'Nei',
}

export interface IAktivitetspliktUnntak {
  id: string
  sakId: number
  behandlingId: string | undefined
  oppgaveId: string
  unntak: AktivitetspliktUnntakType
  fom: string
  tom: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
}

export interface IOpprettAktivitetspliktAktivitetsgrad {
  id: string | undefined
  aktivitetsgrad: AktivitetspliktVurderingType
  fom: string
  tom?: string
  beskrivelse: string
  skjoennsmessigVurdering?: AktivitetspliktSkjoennsmessigVurdering
  vurdertFra12Mnd: boolean
}

export enum AktivitetspliktOppgaveVurderingType {
  SEKS_MAANEDER = 'SEKS_MAANEDER',
  TOLV_MAANEDER = 'TOLV_MAANEDER',
}

export interface IOpprettAktivitetspliktUnntak {
  id: string | undefined
  unntak: AktivitetspliktUnntakType
  fom: string
  tom?: string
  beskrivelse: string
}

export interface AktivitetspliktVurderingValues {
  aktivitetsplikt: JaNei | null
  aktivitetsgrad: AktivitetspliktVurderingType | ''
  unntak: JaNei | null
  midlertidigUnntak: AktivitetspliktUnntakType | ''
  fom?: Date | null
  tom?: Date | null
  beskrivelse: string
}

export const AktivitetspliktVurderingValuesDefault: AktivitetspliktVurderingValues = {
  aktivitetsplikt: null,
  aktivitetsgrad: '',
  unntak: null,
  midlertidigUnntak: '',
  fom: new Date(),
  tom: undefined,
  beskrivelse: '',
}

export interface AktivitetspliktOppgaveVurdering {
  vurderingType: AktivitetspliktOppgaveType
  oppgave: OppgaveDTO
  sak: ISak
  vurdering: IAktivitetspliktVurderingNyDto
  aktivtetspliktbrevdata?: IBrevAktivitetspliktDto
  sistEndret?: string
}

export enum AktivitetspliktOppgaveType {
  SEKS_MAANEDER = 'SEKS_MAANEDER',
  TOLV_MAANEDER = 'TOLV_MAANEDER',
}
