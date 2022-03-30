import { StatusIcon } from '../../../../shared/icons/statusIcon'
import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarWrapper,
  StatusColumn,
} from '../styled'
import { VilkaarProps } from '../types'
import { KriterieOpplysningsType, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import { KildeDatoOpplysning, KildeDatoVilkaar } from './KildeDatoOpplysning'
import { AutomaticIcon } from '../../../../shared/icons/automaticIcon'

export const AlderBarn = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const barnetsFoedselsdato = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
    KriterieOpplysningsType.FOEDSELSDATO
  )
  const avdoedDoedsdato = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
    KriterieOpplysningsType.DOEDSDATO
  )

  const barnetsAlderVedDoedsfall = differenceInYears(
    new Date(avdoedDoedsdato?.opplysning.doedsdato),
    new Date(barnetsFoedselsdato?.opplysning.foedselsdato)
  )

  const barnetsAlder = differenceInYears(new Date(), new Date(barnetsFoedselsdato?.opplysning.foedselsdato))

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <StatusColumn>
            <StatusIcon status={props.vilkaar.resultat} large={true} />
          </StatusColumn>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Alder barn</Title>
              <Lovtekst>§ 18-4: Barnet er under 18 år</Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets fødselsdato</strong>
              </div>
              <div>
                {barnetsFoedselsdato ? (
                  <>
                    <div>
                      {format(new Date(barnetsFoedselsdato?.opplysning.foedselsdato), 'dd.MM.yyyy')}{' '}
                      {barnetsAlder && <span>({barnetsAlder} år)</span>}
                    </div>
                    <KildeDatoOpplysning
                      type={barnetsFoedselsdato?.kilde.type}
                      dato={barnetsFoedselsdato?.kilde.tidspunktForInnhenting}
                    />
                  </>
                ) : (
                  <span className="missing">mangler</span>
                )}
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
            <VilkaarlisteTitle>
              <AutomaticIcon /> {vilkaarErOppfylt(vilkaar.resultat)}
            </VilkaarlisteTitle>
            <KildeDatoVilkaar type={'automatisk'} dato={new Date()} />
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
