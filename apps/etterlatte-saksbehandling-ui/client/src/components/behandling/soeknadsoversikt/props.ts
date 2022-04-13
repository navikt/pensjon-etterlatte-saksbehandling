import { IGyldighetproving, IGyldighetResultat } from '../../../store/reducers/BehandlingReducer'
import {
  IAvdoedFraSoeknad,
  IBarnFraSoeknad,
  IGjenlevendeFraSoeknad,
  IPersonOpplysning,
  IPersonOpplysningFraPdl,
} from '../types'

export interface PropsOmSoeknad {
  gyldighet: IGyldighetResultat
  avdoedPersonPdl: IPersonOpplysningFraPdl
  innsender: IPersonOpplysning
  mottattDato: string
  gjenlevendePdl: IPersonOpplysningFraPdl
  gjenlevendeHarForeldreansvar: IGyldighetproving | undefined
  gjenlevendeOgSoekerLikAdresse: IGyldighetproving | undefined
  innsenderHarForeldreAnsvar: IGyldighetproving | undefined
}

export interface PropsFamilieforhold {
  soekerPdl: IPersonOpplysningFraPdl
  soekerSoknad: IBarnFraSoeknad
  avdoedPersonPdl: IPersonOpplysningFraPdl
  avdodPersonSoknad: IAvdoedFraSoeknad
  gjenlevendePdl: IPersonOpplysningFraPdl
  gjenlevendeSoknad: IGjenlevendeFraSoeknad
  innsender: IPersonOpplysning
}
