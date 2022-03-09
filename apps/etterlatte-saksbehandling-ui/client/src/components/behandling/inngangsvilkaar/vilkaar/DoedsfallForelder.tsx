import format from 'date-fns/format'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import {
  IKriterie,
  IPerson,
  IVilkaaropplysing,
  Kriterietype,
  OpplysningsType,
} from '../../../../store/reducers/BehandlingReducer'
import {
  Innhold,
  Title,
  VilkaarBorder,
  VilkaarVurderingColumn,
  VilkaarColumn,
  VilkaarWrapper,
  VilkaarInfobokser,
  VilkaarlisteTitle,
} from '../styled'
import { VilkaarProps } from '../types'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'

export const DoedsFallForelder = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.avdoed_doedsfall
    )

  const forelder = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER)
    .basertPaaOpplysninger.find(
      (opplysning: IVilkaaropplysing) => opplysning.opplysningsType === OpplysningsType.soeker_relasjon_foreldre
    )

  const avdoedForelder = forelder?.opplysning.foreldre.find(
    (forelder: IPerson) => forelder?.foedselsnummer === avdoedDoedsdato?.opplysning.foedselsnummer
  )

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon status={props.vilkaar.resultat} large={true} /> Dødsfall forelder
        </Title>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
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
                {avdoedForelder ? (
                  <>
                    <div>
                      {avdoedForelder.fornavn} {avdoedForelder.etternavn}
                    </div>
                  </>
                ) : (
                  <span className="missing">mangler navn</span>
                )}
              </div>
              <div>
                {avdoedDoedsdato?.opplysning?.foedselsnummer ? (
                  avdoedDoedsdato.opplysning.foedselsnummer
                ) : (
                  <span className="missing">mangler fødselsnummer</span>
                )}
              </div>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>{vilkaarErOppfylt(props.vilkaar.resultat)}</VilkaarlisteTitle>
            <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
