import { VilkaarColumn } from '../../styled'
import { KildeDatoOpplysning } from '../KildeDatoOpplysning'
import {
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning } from '../../../felles/utils'
import { formaterStringDato } from '../../../../../utils/formattering'

export const InnOgUtvandring = ({ vilkaar }: { vilkaar: IVilkaarsproving | undefined }) => {
  const innOgUtvandring: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_INGEN_INN_ELLER_UTVANDRING,
    KriterieOpplysningsType.UTLAND
  )

  interface Utflytting {
    tilflyttingsland: string
    dato: string
  }

  interface Innflytting {
    fraflyttingsland: string
    dato: string
  }

  const utflytting: string[] = innOgUtvandring?.opplysning.utflyttingFraNorge.map((utflytting: Utflytting) =>
    formaterStringDato(utflytting.dato)
  )
  const innflytting: string[] = innOgUtvandring?.opplysning.innflyttingTilNorge.map((innflytting: Innflytting) =>
    formaterStringDato(innflytting.dato)
  )

  return (
    <VilkaarColumn>
      <div>
        <strong>Inn- og utvanding</strong>
        <div>
          Utflytting fra Norge:{' '}
          {utflytting.map((dato, i) => (
            <div key={i}>{dato}</div>
          ))}
        </div>
        <div>
          Innfytting til Norge:{' '}
          {innflytting.map((dato, i) => (
            <div key={i}>{dato}</div>
          ))}
        </div>
        <KildeDatoOpplysning type={innOgUtvandring?.kilde.type} dato={innOgUtvandring?.kilde.tidspunktForInnhenting} />
      </div>
    </VilkaarColumn>
  )
}
