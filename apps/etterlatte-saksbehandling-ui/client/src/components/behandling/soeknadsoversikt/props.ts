import {
  IAvdoedFraSoeknad,
  IBarnFraSoeknad,
  IGjenlevendeFraSoeknad,
  IPersonOpplysning,
  IPersonOpplysningFraPdl,
} from '../types'

export interface PropsOmSoeknad {
  soekerPdl: IPersonOpplysningFraPdl
  soekerSoknad: IBarnFraSoeknad
  avdoedPersonPdl: IPersonOpplysningFraPdl
  avdodPersonSoknad: IAvdoedFraSoeknad
  innsender: IPersonOpplysning
  mottattDato: string
  avdoedErForelderVilkaar: boolean
}

export interface PropsFamilieforhold {
  soekerPdl: IPersonOpplysningFraPdl
  soekerSoknad: IBarnFraSoeknad
  avdoedPersonPdl: IPersonOpplysningFraPdl
  avdodPersonSoknad: IAvdoedFraSoeknad
  gjenlevendePdl: IPersonOpplysningFraPdl
  gjenlevendeSoknad: IGjenlevendeFraSoeknad
}
