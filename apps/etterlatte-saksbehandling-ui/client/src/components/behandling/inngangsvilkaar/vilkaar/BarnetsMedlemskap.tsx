import { VilkaarProps } from '../types'
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
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { capitalize, vilkaarErOppfylt } from './utils'
import { VilkaarVurderingEnkeltElement, VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import {
  IKriterieOpplysning,
  KriterieOpplysningsType,
  Kriterietype,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { Adressevisning } from '../../felles/Adressevisning'
import {
  hentAdresserEtterDoedsdato,
  hentKriterie,
  hentKriterieOpplysning,
  hentUtenlandskAdresse,
} from '../../felles/utils'
import { KildeDatoOpplysning, KildeDatoVilkaar } from './KildeDatoOpplysning'
import { IAdresse } from '../../types'

export const BarnetsMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const soekerKriterie = hentKriterie(vilkaar, Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET)
  const avdoedDoedsdato = hentKriterieOpplysning(soekerKriterie, KriterieOpplysningsType.DOEDSDATO)?.opplysning
  const adresserBarn = hentKriterieOpplysning(soekerKriterie, KriterieOpplysningsType.ADRESSER)
  const barnUtlandSoeknad = hentKriterieOpplysning(soekerKriterie, KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD)

  const bostedEtterDoedsdato = hentAdresserEtterDoedsdato(adresserBarn?.opplysning?.bostedadresse, avdoedDoedsdato)
  const oppholdEtterDoedsdato = hentAdresserEtterDoedsdato(adresserBarn?.opplysning?.oppholdadresse, avdoedDoedsdato)
  const kontaktEtterDoedsdato = hentAdresserEtterDoedsdato(adresserBarn?.opplysning?.kontaktadresse, avdoedDoedsdato)

  const gjenlevendeKriterie = hentKriterie(vilkaar, Kriterietype.GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET)
  const adresserGjenlevendePdl = hentKriterieOpplysning(gjenlevendeKriterie, KriterieOpplysningsType.ADRESSER)
  const gjenlevendeSkalVises = gjenlevendeKriterie.resultat !== VurderingsResultat.OPPFYLT

  const gjenlevendeBosted = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.bostedadresse, avdoedDoedsdato)
  const gjenlevendeOpphold = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.oppholdadresse, avdoedDoedsdato)
  const gjenlevendeKontakt = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.kontaktadresse, avdoedDoedsdato)

  function lagVilkaarVisning() {
    if (gjenlevendeKriterie.resultat === VurderingsResultat.OPPFYLT) {
      return <VilkaarVurderingsliste kriterie={[soekerKriterie]} />
    } else {
      const tittel = 'Barnet er medlem i trygden'
      let svar
      if (soekerKriterie.resultat === VurderingsResultat.OPPFYLT) {
        svar = 'Avklar. Gjenlevende har utenlandsk adresse'
      } else if (soekerKriterie.resultat === VurderingsResultat.IKKE_OPPFYLT) {
        svar = 'Nei. Barnet har utenlandsk bostedsadresse'
      } else {
        svar = 'Avklar. Barnet og gjenlevende har utenlandsk adresse'
      }
      return <VilkaarVurderingEnkeltElement tittel={tittel} svar={svar} />
    }
  }

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Barnets medlemskap</Title>
              <Lovtekst>§ 18-3: Barnet er medlem av trygden/bosatt i Norge fra dødsfalltidspunktet til i dag</Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets bostedadresse</strong>
                <Adressevisning adresser={bostedEtterDoedsdato} />
                <KildeDatoOpplysning
                  type={adresserBarn?.kilde.type}
                  dato={adresserBarn?.kilde.tidspunktForInnhenting}
                />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets oppholdsadresse</strong>
                <Adressevisning adresser={oppholdEtterDoedsdato} />
                <KildeDatoOpplysning
                  type={adresserBarn?.kilde.type}
                  dato={adresserBarn?.kilde.tidspunktForInnhenting}
                />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets kontaktadresse</strong>
                <Adressevisning adresser={kontaktEtterDoedsdato} />
                <KildeDatoOpplysning
                  type={adresserBarn?.kilde.type}
                  dato={adresserBarn?.kilde.tidspunktForInnhenting}
                />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Bor barnet i et annet land enn Norge?</strong>
                <div>{capitalize(barnUtlandSoeknad?.opplysning.adresseIUtlandet)}</div>
                {barnUtlandSoeknad?.opplysning.adresseIUtlandet === 'JA' && (
                  <>
                    <div>{barnUtlandSoeknad?.opplysning.adresse}</div>
                    <div>{barnUtlandSoeknad?.opplysning.land}</div>
                  </>
                )}
                <KildeDatoOpplysning type={barnUtlandSoeknad?.kilde.type} dato={barnUtlandSoeknad?.kilde.mottatDato} />
              </div>
            </VilkaarColumn>
            {gjenlevendeSkalVises && (
              <>
                <GjenlevendeAdresser
                  adresse={gjenlevendeBosted}
                  tittel={'bostedsadresse'}
                  kritOpp={adresserGjenlevendePdl}
                />
                <GjenlevendeAdresser
                  adresse={gjenlevendeOpphold}
                  tittel={'oppholdsadresse'}
                  kritOpp={adresserGjenlevendePdl}
                />
                <GjenlevendeAdresser
                  adresse={gjenlevendeKontakt}
                  tittel={'kontaktadresse'}
                  kritOpp={adresserGjenlevendePdl}
                />
              </>
            )}
          </VilkaarInfobokser>

          <VilkaarVurderingColumn>
            {vilkaar != undefined && (
              <VilkaarVurderingContainer>
                <VilkaarlisteTitle>
                  <StatusIcon status={vilkaar.resultat} large={true} />{' '}
                  {props.vilkaar?.resultat && vilkaarErOppfylt(props.vilkaar.resultat)}
                </VilkaarlisteTitle>
                <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
                {lagVilkaarVisning()}
              </VilkaarVurderingContainer>
            )}
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}

const GjenlevendeAdresser = ({
  adresse,
  tittel,
  kritOpp,
}: {
  adresse?: IAdresse[]
  tittel: String
  kritOpp: IKriterieOpplysning | undefined
}) => {
  return (
    <>
      {adresse != undefined && adresse.length > 0 && (
        <VilkaarColumn>
          <div>
            <strong>Gjenlevendes {tittel}</strong>
            <Adressevisning adresser={adresse} />
            <KildeDatoOpplysning type={kritOpp?.kilde?.type} dato={kritOpp?.kilde?.tidspunktForInnhenting} />
          </div>
        </VilkaarColumn>
      )}
    </>
  )
}
