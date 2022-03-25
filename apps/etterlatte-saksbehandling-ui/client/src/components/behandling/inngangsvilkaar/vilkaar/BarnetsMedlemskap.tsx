import { VilkaarProps } from '../types'
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
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { capitalize, vilkaarErOppfylt } from './utils'
import { VilkaarVurderingEnkeltElement, VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import {
  IKriterie,
  IKriterieOpplysning,
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarVurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { Adressevisning } from '../../felles/Adressevisning'
import {
  hentAdresserEtterDoedsdato,
  hentKriterie,
  hentKriterieOpplysning,
  hentUtenlandskAdresse,
} from '../../felles/utils'
import { KildeDato } from './KildeDato'
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
  const gjenlevendeSkalVises = gjenlevendeKriterie.resultat !== VilkaarVurderingsResultat.OPPFYLT

  const gjenlevendeBosted = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.bostedadresse, avdoedDoedsdato)
  const gjenlevendeOpphold = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.oppholdadresse, avdoedDoedsdato)
  const gjenlevendeKontakt = hentUtenlandskAdresse(adresserGjenlevendePdl?.opplysning.kontaktadresse, avdoedDoedsdato)

  function lagVilkaarVisning() {
    if (gjenlevendeKriterie.resultat === VilkaarVurderingsResultat.OPPFYLT) {
      return <VilkaarVurderingsliste kriterie={[soekerKriterie]} />
    } else {
      const tittel = 'Barnet er medlem i trygden'
      let svar
      let resultat
      if (soekerKriterie.resultat === VilkaarVurderingsResultat.OPPFYLT) {
        svar = 'Avklar. Gjenlevende har utenlandsk adresse'
        resultat = VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
      } else if (soekerKriterie.resultat === VilkaarVurderingsResultat.IKKE_OPPFYLT) {
        svar = 'Nei. Barnet har utenlandsk bostedsadresse'
        resultat = VilkaarVurderingsResultat.IKKE_OPPFYLT
      } else {
        svar = 'Avklar. Barnet og gjenlevende har utenlandsk adresse'
        resultat = VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
      }
      return <VilkaarVurderingEnkeltElement tittel={tittel} svar={svar} resultat={resultat} />
    }
  }

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon status={props.vilkaar.resultat} large={true} />
          Barnets medlemskap
        </Title>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <div>§ 18-3</div>
              <div>Barnet er medlem av trygden/bosatt i Norge fra dødsfalltidspunktet til i dag</div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets bostedadresse</strong>
                <Adressevisning adresser={bostedEtterDoedsdato} />
                <KildeDato type={adresserBarn?.kilde.type} dato={adresserBarn?.kilde.tidspunktForInnhenting} />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets oppholdsadresse</strong>
                <Adressevisning adresser={oppholdEtterDoedsdato} />
                <KildeDato type={adresserBarn?.kilde.type} dato={adresserBarn?.kilde.tidspunktForInnhenting} />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Barnets kontaktadresse</strong>
                <Adressevisning adresser={kontaktEtterDoedsdato} />
                <KildeDato type={adresserBarn?.kilde.type} dato={adresserBarn?.kilde.tidspunktForInnhenting} />
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
                <KildeDato type={barnUtlandSoeknad?.kilde.type} dato={barnUtlandSoeknad?.kilde.mottattDato} />
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
            <VilkaarlisteTitle>{props.vilkaar?.resultat && vilkaarErOppfylt(props.vilkaar.resultat)}</VilkaarlisteTitle>
            {lagVilkaarVisning()}
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
      {adresse == undefined || adresse.length == 0 ? (
        <div></div>
      ) : (
        <VilkaarColumn>
          <div>
            <strong>Gjenlevendes {tittel}</strong>
            <Adressevisning adresser={adresse} />
            <KildeDato type={kritOpp?.kilde?.type} dato={kritOpp?.kilde?.tidspunktForInnhenting} />
          </div>
        </VilkaarColumn>
      )}
    </>
  )
}
