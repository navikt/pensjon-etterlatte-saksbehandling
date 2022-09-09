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
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { hentKriterie, hentKriterierMedOpplysning } from '../../../felles/utils'
import { formaterStringDato } from '../../../../../utils/formattering'

export const AvdoedesForutMedlemskap = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  const [isOpen, setIsOpen] = useState(false)

  const statsborgerskap: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_NORSK_STATSBORGER,
    KriterieOpplysningsType.STATSBORGERSKAP
  )

  const innOgUtvandring: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_INGEN_INN_ELLER_UTVANDRING,
    KriterieOpplysningsType.UTLAND
  )

  const utflytting: string[] = innOgUtvandring?.opplysning.utflyttingFraNorge.map(
    (utflytting: { fraflyttingsland: string; dato: string }) => formaterStringDato(utflytting.dato)
  )
  const innflytting: string[] = innOgUtvandring?.opplysning.innflyttingTilNorge.map(
    (innflytting: { tilflyttingsland: string; dato: string }) => formaterStringDato(innflytting.dato)
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
                <div>{statsborgerskap?.opplysning}</div>
                <KildeDatoOpplysning
                  type={statsborgerskap?.kilde.type}
                  dato={statsborgerskap?.kilde.tidspunktForInnhenting}
                />
              </div>
            </VilkaarColumn>
            {hentKriterie(vilkaar, Kriterietype.AVDOED_INGEN_INN_ELLER_UTVANDRING)?.resultat !=
              VurderingsResultat.OPPFYLT && (
              <VilkaarColumn>
                <div>
                  <strong>Inn- og utvanding</strong>
                  <div>
                    Utflytting fra Norge:{' '}
                    {utflytting.map((dato, i) => (
                      <div key={i}>{dato}</div>
                    ))}
                  </div>
                  <div>
                    Innfytting til Norge:{' '}
                    {innflytting.map((dato, i) => (
                      <div key={i}>{dato}</div>
                    ))}
                  </div>
                  <KildeDatoOpplysning
                    type={innOgUtvandring?.kilde.type}
                    dato={innOgUtvandring?.kilde.tidspunktForInnhenting}
                  />
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
      </Innhold>
      {isOpen && <PeriodeModal isOpen={isOpen} setIsOpen={(value: boolean) => setIsOpen(value)} />}
    </VilkaarBorder>
  )
}
