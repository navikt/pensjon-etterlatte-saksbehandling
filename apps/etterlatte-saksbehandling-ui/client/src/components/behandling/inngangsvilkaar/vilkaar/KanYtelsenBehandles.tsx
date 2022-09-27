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
import { vilkaarErOppfylt } from './tekstUtils'
import { KildeDatoVilkaar } from './KildeDatoOpplysning'
import { VilkaarVurderingsliste } from './VilkaarVurderingsliste'
import React from 'react'
import { Opphoersgrunn, OPPHOERSGRUNNER, OVERSETTELSER_OPPHOERSGRUNNER } from '../../../person/ManueltOpphoerModal'

function erOversettelseGrunn(tekst: string): tekst is Opphoersgrunn {
  return (OPPHOERSGRUNNER as readonly string[]).includes(tekst)
}

export const KanYtelsenBehandles = (props: VilkaarProps) => {
  const vilkaar = props.vilkaar
  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>Kan løsningen revurdere</Title>
              <Lovtekst>Støtter systemet å gjøre den nødvendige revurderingen på denne saken</Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Grunner til opphør</strong>
              </div>
              <ul>
                {vilkaar?.kriterier?.flatMap((kriterie) =>
                  kriterie.basertPaaOpplysninger.map((opplysning) => (
                    <li key={opplysning.opplysning}>
                      {erOversettelseGrunn(opplysning.opplysning)
                        ? OVERSETTELSER_OPPHOERSGRUNNER[opplysning.opplysning]
                        : opplysning.opplysning}
                    </li>
                  ))
                )}
              </ul>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn>
            {vilkaar != undefined && (
              <VilkaarVurderingContainer>
                <VilkaarlisteTitle>
                  <StatusIcon status={vilkaar.resultat} large={true} /> {vilkaarErOppfylt(vilkaar.resultat)}
                </VilkaarlisteTitle>
                <KildeDatoVilkaar isHelautomatisk={false} dato={vilkaar.vurdertDato} />
                <VilkaarVurderingsliste kriterie={vilkaar.kriterier} />
              </VilkaarVurderingContainer>
            )}
          </VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
