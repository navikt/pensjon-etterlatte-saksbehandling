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
  mottattDato: string
  innsenderHarForeldreansvar: IGyldighetproving | undefined
  gjenlevendeOgSoekerLikAdresse: IGyldighetproving | undefined
  innsenderErForelder: IGyldighetproving | undefined
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
