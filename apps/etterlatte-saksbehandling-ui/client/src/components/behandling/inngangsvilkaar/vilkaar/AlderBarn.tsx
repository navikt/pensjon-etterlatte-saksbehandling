import { StatusIcon } from '../../../../shared/icons/statusIcon'
import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import {
  Innhold,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
} from '../styled'
import { VilkaarProps } from '../types'
import { KriterieOpplysningsType, Kriterietype, OpplysningsType } from '../../../../store/reducers/BehandlingReducer'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import { hentKriterier } from '../../felles/utils'

export const AlderBarn = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const barnetsFoedselsdato = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
    KriterieOpplysningsType.FOEDSELSDATO
  )
  const avdoedDoedsdato = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
    KriterieOpplysningsType.DOEDSDATO
  )

  const barnetsAlderVedDoedsfall = differenceInYears(
    new Date(avdoedDoedsdato.opplysning.doedsdato),
    new Date(barnetsFoedselsdato.opplysning.foedselsdato)
  )

  const barnetsAlder = differenceInYears(new Date(), new Date(barnetsFoedselsdato.opplysning.foedselsdato))

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon status={props.vilkaar.resultat} large={true} /> Alder barn
        </Title>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
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
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>{vilkaarErOppfylt(vilkaar.resultat)}</VilkaarlisteTitle>
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
