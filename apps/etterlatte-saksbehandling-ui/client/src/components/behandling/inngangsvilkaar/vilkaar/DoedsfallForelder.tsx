import format from 'date-fns/format'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { IKriterie, IVilkaaropplysing, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { Innhold, Title, VilkaarBorder, VilkaarVurderingColumn, VilkaarColumn, VilkaarWrapper } from '../styled'
import { OpplysningsType } from '../types'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'

export const DoedsFallForelder = (props: any) => {
  const vilkaar = props.vilkaar

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.avdoed_doedsfall
    )

  const forelder = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.relasjon_foreldre
    )

  return (
    <VilkaarBorder>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarColumn>
            <Title>
              <StatusIcon status={props.vilkaar.resultat} /> Dødsfall forelder
            </Title>
            <div>§ 18-5</div>
            <div>En eller begge foreldrene døde</div>
          </VilkaarColumn>
          <VilkaarColumn>
            <div>
              <strong>Dødsdato</strong>
            </div>
            <div>
              {avdoedDoedsdato?.opplysning?.doedsdato ? (
                format(new Date(avdoedDoedsdato.opplysning.doedsdato), 'dd.MM.yyyy')
              ) : (
                <span className="missing">mangler</span>
              )}
            </div>
          </VilkaarColumn>
          <VilkaarColumn>
            <div>
              <strong>Avdød forelder</strong>
            </div>
            <div>
              {forelder?.opplysning?.foreldre ? forelder.opplysning.foreldre : <span className="missing">mangler</span>}
            </div>
            <div>
              {avdoedDoedsdato?.opplysning?.foedselsnummer ? (
                avdoedDoedsdato.opplysning.foedselsnummer
              ) : (
                <span className="missing">mangler</span>
              )}
            </div>
          </VilkaarColumn>
          <VilkaarVurderingColumn>
            <Title>{vilkaarErOppfylt(props.vilkaar.resultat)}</Title>
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
