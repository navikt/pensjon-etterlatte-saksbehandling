import React from 'react'
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
import { format } from 'date-fns'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import { KriterieOpplysningsType, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { vilkaarErOppfylt } from './tekstUtils'
import { KildeDatoOpplysning, KildeDatoVilkaar } from './KildeDatoOpplysning'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'

export const Formaal = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  const soekerDoedsdato = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.SOEKER_ER_I_LIVE,
    KriterieOpplysningsType.DOEDSDATO
  )
  if (soekerDoedsdato?.opplysning == undefined) {
    return null
  }

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>§ 1-1: Formål</Title>
              <Lovtekst>
                Barnepensjon har som formål å dekke utgifter til livsopphold. Når personen som mottar ytelsen dør,
                faller grunnlaget for opprettholdelse av ytelsen bort.
              </Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Dødsfall registrert</strong>
              </div>
              <div>
                {soekerDoedsdato ? (
                  <>
                    <div>{format(new Date(soekerDoedsdato?.opplysning.doedsdato), 'dd.MM.yyyy')}</div>
                    <KildeDatoOpplysning
                      type={soekerDoedsdato?.kilde.type}
                      dato={soekerDoedsdato?.kilde.tidspunktForInnhenting}
                    />
                  </>
                ) : (
                  <span className="mangler">Søker er i live</span>
                )}
              </div>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            <VilkaarVurderingContainer>
              {vilkaar != undefined ? (
                <>
                  <VilkaarlisteTitle>
                    <StatusIcon status={vilkaar.resultat} large /> {vilkaarErOppfylt(vilkaar.resultat)}
                  </VilkaarlisteTitle>
                  <KildeDatoVilkaar isHelautomatisk={true} dato={vilkaar.vurdertDato} />
                  <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
                </>
              ) : null}
            </VilkaarVurderingContainer>
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
