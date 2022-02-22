import { StatusIcon } from '../../../../shared/icons/statusIcon'
import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import { Title, VilkaarColumn, VilkaarWrapper } from '../styled'
import { OpplysningsType } from '../types'
import { IBehandlingsopplysning, IKriterie, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { VilkaarIkkeOppfylt } from './VilkaarIkkeOppfylt'

export const AlderBarn = (props: any) => {
  const vilkaar = props.vilkaar

  const barnetsFoedselsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO)
    .basertPaaOpplysninger.find(
      (opplysning: IBehandlingsopplysning) => opplysning.opplysningsType === OpplysningsType.soeker_foedselsdato
    )

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO)
    .basertPaaOpplysninger.find(
      (opplysning: IBehandlingsopplysning) => opplysning.opplysningsType === OpplysningsType.avdoed_doedsfall
    )

  const barnetsAlder = differenceInYears(
    new Date(avdoedDoedsdato.opplysning.doedsdato),
    new Date(barnetsFoedselsdato.opplysning.foedselsdato)
  )

  return (
    <div style={{ borderBottom: '1px solid #ccc', padding: '1em 0' }}>
      <VilkaarWrapper>
        <VilkaarColumn>
          <Title>
            <StatusIcon status={props.vilkaar.resultat} /> Alder barn
          </Title>
          <div>§ 18-5</div>
          <div>Barnet er under 20 år</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Barnets fødselsdato</strong>
          </div>
          <div>{format(new Date(barnetsFoedselsdato.opplysning.foedselsdato), 'dd.MM.yyyy')}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Alder ved dødsfall</strong>
          </div>
          <div>{barnetsAlder ? `${barnetsAlder} år` : <span className="missing">mangler</span>}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <Title>
            Vilkår er {props.vilkaar.resultat === VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ? (
              <> ikke oppfyllt</>
            ) : (
              <> oppfyllt</>
            )}
          </Title>
          <VilkaarIkkeOppfylt status={props.vilkaar.resultat} errorText='Vi har bla bla bla fått bla bla bla som sier at bla'/>
        </VilkaarColumn>
      </VilkaarWrapper>
    </div>
  )
}
