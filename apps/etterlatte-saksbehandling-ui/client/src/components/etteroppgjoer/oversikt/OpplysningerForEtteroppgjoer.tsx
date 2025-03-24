import { Heading, Table, VStack } from '@navikt/ds-react'
import React from 'react'
import { AInntekt, EtteroppgjoerOpplysninger, PensjonsgivendeInntektFraSkatteetaten } from '~shared/types/Etteroppgjoer'
import { NOK } from '~utils/formatering/formatering'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'

export const OpplysningerForEtteroppgjoer = ({ opplysninger }: { opplysninger: EtteroppgjoerOpplysninger }) => {
  return (
    <>
      <VStack maxWidth="80rem">
        <Heading size="small">Opplysninger skatt</Heading>
        <SkattTabell skatt={opplysninger.skatt} />
      </VStack>
      <VStack maxWidth="50rem">
        <Heading size="small">Opplysninger A-Inntekt</Heading>
        <AInntektTabell ainntekt={opplysninger.ainntekt} />
      </VStack>
      <VStack maxWidth="80rem">
        <AvkortingInntektTabell
          avkortingGrunnlagListe={opplysninger.tidligereAvkorting.avkortingGrunnlag}
          fyller67={false}
        />
      </VStack>
      <VStack>
        <YtelseEtterAvkorting
          avkortetYtelse={opplysninger.tidligereAvkorting.avkortetYtelse}
          tidligereAvkortetYtelse={[]}
        />
      </VStack>
    </>
  )
}

const SkattTabell = ({ skatt }: { skatt: PensjonsgivendeInntektFraSkatteetaten }) => {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Skatteordning</Table.HeaderCell>
          <Table.HeaderCell>Lønnsinntekt</Table.HeaderCell>
          <Table.HeaderCell>Næringsinntekt</Table.HeaderCell>
          <Table.HeaderCell>Fiske, fangst og familiebarnehage</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {skatt.inntekter.map((inntekt, i) => {
          return (
            <Table.Row key={i}>
              <Table.DataCell>{inntekt.skatteordning}</Table.DataCell>
              <Table.DataCell>{NOK(inntekt.loensinntekt)}</Table.DataCell>
              <Table.DataCell>{NOK(inntekt.naeringsinntekt)}</Table.DataCell>
              <Table.DataCell>{NOK(inntekt.annet)}</Table.DataCell>
            </Table.Row>
          )
        })}
      </Table.Body>
    </Table>
  )
}

const AInntektTabell = ({ ainntekt }: { ainntekt: AInntekt }) => {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Måned</Table.HeaderCell>
          <Table.HeaderCell>Summert inntekt</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {ainntekt.inntektsmaaneder.map((maaned, i) => {
          return (
            <Table.Row key={i}>
              <Table.DataCell>{maaned.maaned}</Table.DataCell>
              <Table.DataCell>{NOK(maaned.summertBeloep)}</Table.DataCell>
            </Table.Row>
          )
        })}
      </Table.Body>
    </Table>
  )
}
