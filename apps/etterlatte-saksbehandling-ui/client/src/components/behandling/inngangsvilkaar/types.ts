import { IVilkaarsproving } from '../../../store/reducers/BehandlingReducer'

export interface VilkaarProps {
  id: string
  vilkaar: IVilkaarsproving | undefined
}

export enum IPeriodeType {
  velg = 'Velg',
  arbeidsperiode = 'Arbeidsperiode',
  dagpenger = 'Dagpenger',
}

export interface IPeriodeInput {
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
