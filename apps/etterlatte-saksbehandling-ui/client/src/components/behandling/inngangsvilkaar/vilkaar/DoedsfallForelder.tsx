import format from 'date-fns/format'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import {
  IPerson,
  KriterieOpplysningsType,
  Kriterietype,
  OpplysningsType,
} from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarlisteTitle,
  VilkaarVurderingColumn,
  VilkaarVurderingContainer,
  VilkaarWrapper,
} from '../styled'
import { VilkaarProps } from '../types'
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import { KildeDatoOpplysning, KildeDatoVilkaar } from './KildeDatoOpplysning'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'

export const DoedsFallForelder = (props: VilkaarProps) => {
  const ctx = useContext(AppContext)
  const grunnlag = ctx.state.behandlingReducer.grunnlag

  const vilkaar = props.vilkaar

  const avdoedDoedsdato: any = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL,
    KriterieOpplysningsType.DOEDSDATO
  )
  const forelder: any = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_ER_FORELDER,
    KriterieOpplysningsType.FORELDRE
  )

  const avdoedForelderFnr = forelder?.opplysning.foreldre.find(
    (forelder: IPerson) => forelder === avdoedDoedsdato?.opplysning.foedselsnummer
  )

  const avdoedForelderGrunnlag = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_forelder_pdl
  )?.opplysning

  const avdoedNavn = avdoedForelderFnr ? (
    <div>
      {avdoedForelderGrunnlag?.fornavn} {avdoedForelderGrunnlag?.etternavn}
    </div>
  ) : (
    <span className="missing">mangler navn</span>
  )

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Dødsfall forelder</Title>
              <Lovtekst>§ 18-4: En eller begge foreldrene døde</Lovtekst>
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
              <KildeDatoOpplysning
                type={avdoedDoedsdato?.kilde.type}
                dato={avdoedDoedsdato?.kilde.tidspunktForInnhenting}
              />
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Avdød forelder</strong>
              </div>
              <div>{avdoedNavn}</div>
              <div>
                {avdoedForelderFnr ? avdoedForelderFnr : <span className="missing">mangler fødselsnummer</span>}
              </div>
              {avdoedForelderFnr && (
                <KildeDatoOpplysning
                  type={avdoedDoedsdato?.kilde.type}
                  dato={avdoedDoedsdato?.kilde.tidspunktForInnhenting}
                />
              )}
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              <VilkaarlisteTitle>
                <StatusIcon status={props.vilkaar.resultat} large={true} /> {vilkaarErOppfylt(props.vilkaar.resultat)}
              </VilkaarlisteTitle>
              <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
              <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
