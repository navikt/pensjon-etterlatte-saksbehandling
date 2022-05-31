import { IGyldighetResultat, IKommerSoekerTilgode } from '../../../store/reducers/BehandlingReducer'
import {
  IAvdoedFraSoeknad,
  IBarnFraSoeknad,
  IGjenlevendeFraSoeknad,
  IPersonOpplysning,
  IPersonOpplysningFraPdl,
} from '../types'

export interface PropsOmSoeknad {
  gyldigFramsatt: IGyldighetResultat
  kommerSoekerTilgode: IKommerSoekerTilgode
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
