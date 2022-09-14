import { IVilkaarsproving, KildeType } from '../../../store/reducers/BehandlingReducer'

export interface VilkaarProps {
  id: string
  vilkaar: IVilkaarsproving | undefined
}

export interface ITidslinjePeriode {
  type: TidslinjePeriodeType
  innhold: IAdresseTidslinjePeriodeInnhold | IVurdertPeriode
  kilde: any
}

export enum TidslinjePeriodeType {
  ADRESSE = 'ADRESSE',
  GAP = 'GAP',
  ARBEIDSPERIODE = 'ARBEIDSPERIODE',
  INNTEKT = 'INNTEKT',
}

export interface IAdresseTidslinjePeriodeInnhold {
  fraDato: string
  tilDato?: string
  beskrivelse: string
  adresseINorge: boolean
  land?: string
}

export interface IGap {
  gyldigFra: string
  gyldigTil: string
}

export enum IPeriodeType {
  velg = 'Velg',
  arbeidsperiode = 'Arbeidsperiode',
  alderspensjon = 'Alderspensjon',
  uføretrygd = 'Uføretrygd',
  foreldrepenger = 'Foreldrepenger',
  sykepenger = 'Sykepenger',
  dagpenger = 'Dagpenger',
  arbeidsavklaringspenger = 'Arbeidsavklaringspenger',
}

export interface IPeriodeInput {
  id?: string
  periodeType: IPeriodeType
  arbeidsgiver?: string
  stillingsprosent?: string
  begrunnelse?: string
  kilde?: string
  fraDato: Date | null
  tilDato: Date | null
}

export interface IPeriodeInputErros {
  periodeType?: string
  arbeidsgiver?: string
  stillingsprosent?: string
  kilde?: string
  fraDato?: string
  tilDato?: string
}

export interface IVurdertPeriode {
  id: string
  periodeType: IReturnertPeriodeType
  kilde: ISaksbehandlerKilde | IRegisterKilde
  arbeidsgiver?: string
  stillingsprosent?: string
  begrunnelse?: string
  oppgittKilde?: string
  fraDato: string
  tilDato: string
  beskrivelse?: string
  godkjentPeriode: boolean
}

export interface ISaksbehandlerKilde {
  ident: string
  tidspunkt: string
  type: KildeType
}

export interface IRegisterKilde {
  tidspunkt: string
  type: KildeType
}

export enum IReturnertPeriodeType {
  arbeidsperiode = 'ARBEIDSPERIODE',
  alderspensjon = 'ALDERSPENSJON',
  uføretrygd = 'UFOERETRYGD',
  foreldrepenger = 'FORELDREPENGER',
  sykepenger = 'SYKEPENGER',
  dagpenger = 'DAGPENGER',
  arbeidsavklaringspenger = 'ARBEIDSAVKLARINGSPENGER',
}
