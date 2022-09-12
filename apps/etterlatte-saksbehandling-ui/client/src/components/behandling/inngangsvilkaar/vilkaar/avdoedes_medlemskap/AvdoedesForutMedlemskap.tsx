import { StatusIcon } from '../../../../../shared/icons/statusIcon'
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
} from '../../styled'
import { VilkaarProps } from '../../types'
import { TidslinjeMedlemskap } from './TidslinjeMedlemskap'
import { KildeDatoOpplysning, KildeDatoVilkaar } from '../KildeDatoOpplysning'
import { vilkaarErOppfylt } from '../utils'
import { Button } from '@navikt/ds-react'
import { useState } from 'react'
import { PeriodeModal } from './PeriodeModal'
import {
  IKriterieOpplysning,
  KriterieOpplysningsType,
  Kriterietype,
  Utfall,
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { hentKriterie, hentKriterierMedOpplysning } from '../../../felles/utils'
import { InnOgUtvandring } from './InnOgUtvandring'
import { lagVilkaarVisningAvklaring, lagVilkaarVisningUtland } from './vilkaarVurderingTekst'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  const [isOpen, setIsOpen] = useState(false)

  const statsborgerskap: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_NORSK_STATSBORGER,
    KriterieOpplysningsType.STATSBORGERSKAP
  )

  const utlandSoeknad = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD,
    KriterieOpplysningsType.AVDOED_UTENLANDSOPPHOLD
  )

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title> § 18-2: Avdødes forutgående medlemskap</Title>
              <Lovtekst>
                Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet
              </Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Statsborgerskap</strong>
                {statsborgerskap?.opplysning ? (
                  <>
                    <div>{statsborgerskap?.opplysning}</div>
                    <KildeDatoOpplysning
                      type={statsborgerskap?.kilde.type}
                      dato={statsborgerskap?.kilde.tidspunktForInnhenting}
                    />
                  </>
                ) : (
                  <div className="missing">Mangler</div>
                )}
              </div>
            </VilkaarColumn>

            {hentKriterie(vilkaar, Kriterietype.AVDOED_INGEN_INN_ELLER_UTVANDRING)?.resultat !=
              VurderingsResultat.OPPFYLT && <InnOgUtvandring vilkaar={vilkaar} />}

            {hentKriterie(vilkaar, Kriterietype.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD)?.resultat !=
              VurderingsResultat.OPPFYLT && (
              <VilkaarColumn>
                <div>
                  <strong>Utlandshopphold fra søknad</strong>
                  <div>{utlandSoeknad?.opplysning?.harHattUtenlandsopphold}</div>
                  <KildeDatoOpplysning type={utlandSoeknad?.kilde.type} dato={utlandSoeknad?.kilde.mottatDato} />
                </div>
              </VilkaarColumn>
            )}
          </VilkaarInfobokser>

          {vilkaar != undefined && (
            <VilkaarVurderingColumn>
              <VilkaarVurderingContainer>
                <VilkaarlisteTitle>
                  <StatusIcon status={vilkaar.resultat} large={true} /> {vilkaarErOppfylt(vilkaar.resultat)}
                </VilkaarlisteTitle>
                <KildeDatoVilkaar type={'automatisk'} dato={vilkaar.vurdertDato} />
                {lagVilkaarVisningUtland(vilkaar)}
                {lagVilkaarVisningAvklaring(vilkaar)}
              </VilkaarVurderingContainer>
            </VilkaarVurderingColumn>
          )}
        </VilkaarWrapper>

        {vilkaar != undefined ? (
          <>
            <TidslinjeMedlemskap vilkaar={vilkaar} />
            <Button variant={'secondary'} size={'small'} onClick={() => setIsOpen(true)}>
              + Legg til periode
            </Button>
          </>
        ) : (
          <div>Vilkår mangler</div>
        )}
        {vilkaar?.utfall === Utfall.BEHANDLE_I_PSYS && (
          <Button variant={'primary'} size={'medium'}>
            Behandle i psys (mock)
          </Button>
        )}
      </Innhold>

      {isOpen && <PeriodeModal isOpen={isOpen} setIsOpen={(value: boolean) => setIsOpen(value)} />}
    </VilkaarBorder>
  )
}
