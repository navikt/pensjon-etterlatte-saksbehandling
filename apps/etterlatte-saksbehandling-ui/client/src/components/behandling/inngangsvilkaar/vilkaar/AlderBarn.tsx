import { StatusIcon } from '../../../../shared/icons/statusIcon'
import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import { Title, VilkaarColumn, VilkaarVurderingColumn, VilkaarWrapper, VilkaarBorder, Innhold } from '../styled'
import { OpplysningsType } from '../types'
import { IKriterie, IVilkaaropplysing, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'

export const AlderBarn = (props: any) => {
  const vilkaar = props.vilkaar

  const barnetsFoedselsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.soeker_foedselsdato
    )

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.avdoed_doedsfall
    )

  const barnetsAlderVedDoedsfall = differenceInYears(
    new Date(avdoedDoedsdato.opplysning.doedsdato),
    new Date(barnetsFoedselsdato.opplysning.foedselsdato)
  )

  const barnetsAlder = differenceInYears(new Date(), new Date(barnetsFoedselsdato.opplysning.foedselsdato))

  return (
    <VilkaarBorder>
      <Innhold>
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
            <div>
              {format(new Date(barnetsFoedselsdato.opplysning.foedselsdato), 'dd.MM.yyyy')}{' '}
              {barnetsAlder && <span>({barnetsAlder} år)</span>}
            </div>
          </VilkaarColumn>
          <VilkaarColumn>
            <div>
              <strong>Alder ved dødsfall</strong>
            </div>
            <div>
              {barnetsAlderVedDoedsfall ? `${barnetsAlderVedDoedsfall} år` : <span className="missing">mangler</span>}
            </div>
          </VilkaarColumn>
          <VilkaarVurderingColumn>
            <Title>{vilkaarErOppfylt(vilkaar.resultat)}</Title>
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
