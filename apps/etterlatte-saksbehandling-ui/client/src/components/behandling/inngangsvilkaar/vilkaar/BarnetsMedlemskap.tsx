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
import { vilkaarErOppfylt } from './utils'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import {
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarVurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { Adressevisning } from '../../felles/Adressevisning'
import { hentAdresserEtterDoedsdato, hentKriterier } from '../../felles/utils'

export const BarnetsMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar

  const avdoedDoedsdato = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET,
    KriterieOpplysningsType.DOEDSDATO
  ).opplysning.doedsdato
  const bostedadresser = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET,
    KriterieOpplysningsType.ADRESSER
  )?.opplysning.bostedadresse
  const oppholdsadresser = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET,
    KriterieOpplysningsType.ADRESSER
  )?.opplysning.oppholdsadresse
  const kontaktadresser = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET,
    KriterieOpplysningsType.ADRESSER
  )?.opplysning.kontaktadresse
  const harUtelandsadresse = hentKriterier(
    vilkaar,
    Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET,
    KriterieOpplysningsType.ADRESSER
  )

  const bostedEtterDoedsdato = hentAdresserEtterDoedsdato(bostedadresser, avdoedDoedsdato)
  const oppholdEtterDoedsdato = hentAdresserEtterDoedsdato(oppholdsadresser, avdoedDoedsdato)
  const kontaktEtterDoedsdato = hentAdresserEtterDoedsdato(kontaktadresser, avdoedDoedsdato)

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <Title>
          <StatusIcon
            status={
              props.vilkaar?.resultat ? VilkaarVurderingsResultat.OPPFYLT : VilkaarVurderingsResultat.IKKE_OPPFYLT
            }
            large={true}
          />{' '}
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
                <strong>Bostedadresse</strong>
                <Adressevisning adresser={bostedEtterDoedsdato} />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Oppholdsadresse</strong>
                <Adressevisning adresser={oppholdEtterDoedsdato} />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Kontaktadresse</strong>
                <Adressevisning adresser={kontaktEtterDoedsdato} />
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Utenlandsadresse fra søknad</strong>
                <div>{harUtelandsadresse?.opplysning.adresseIUtlandet}</div>
              </div>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarlisteTitle>{props.vilkaar?.resultat && vilkaarErOppfylt(props.vilkaar.resultat)}</VilkaarlisteTitle>
            <VilkaarVurderingsliste kriterie={props.vilkaar?.kriterier ? vilkaar.kriterier : []} />
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
