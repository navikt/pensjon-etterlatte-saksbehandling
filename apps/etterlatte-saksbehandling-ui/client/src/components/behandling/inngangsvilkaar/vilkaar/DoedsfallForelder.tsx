import format from 'date-fns/format'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { IPerson, KriterieOpplysningsType, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
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
import { vilkaarErOppfylt } from './tekstUtils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import { KildeDatoOpplysning, KildeDatoVilkaar } from './KildeDatoOpplysning'
import { useAppSelector } from '../../../../store/Store'

export const DoedsFallForelder = (props: VilkaarProps) => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
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

  const avdoedPersoninfo = behandling.kommerSoekerTilgode?.familieforhold.avdoed

  const avdoedNavn = avdoedForelderFnr ? (
    <div>{avdoedPersoninfo?.navn}</div>
  ) : (
    <span className="missing">mangler navn</span>
  )

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>§ 18-4: Dødsfall forelder</Title>
              <Lovtekst>En eller begge foreldrene døde</Lovtekst>
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
            {vilkaar != undefined && (
              <VilkaarVurderingContainer>
                <VilkaarlisteTitle>
                  <StatusIcon status={vilkaar.resultat} large={true} /> {vilkaarErOppfylt(vilkaar.resultat)}
                </VilkaarlisteTitle>
                <KildeDatoVilkaar isHelautomatisk={true} dato={vilkaar.vurdertDato} />
                <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
              </VilkaarVurderingContainer>
            )}
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
