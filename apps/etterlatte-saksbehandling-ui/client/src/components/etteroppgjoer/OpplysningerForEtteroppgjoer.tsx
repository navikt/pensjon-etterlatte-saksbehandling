import { Box, Heading, Table, VStack } from '@navikt/ds-react'
import React from 'react'
import { EtteroppgjoerOpplysninger } from '~shared/types/Etteroppgjoer'
import { NOK } from '~utils/formatering/formatering'
import { YtelseEtterAvkorting } from '~components/behandling/avkorting/YtelseEtterAvkorting'
import { AvkortingInntektTabell } from '~components/behandling/avkorting/AvkortingInntektTabell'

export const OpplysningerForEtteroppgjoer = ({ opplysninger }: { opplysninger: EtteroppgjoerOpplysninger }) => {
  return (
    <>
      <VStack paddingBlock="8" paddingInline="16 8">
        <Heading size="small">Opplysninger skatt</Heading>
        <Box>Årsinntekt: {NOK(opplysninger.skatt.aarsinntekt)}</Box>
      </VStack>
      <VStack maxWidth="30rem" paddingBlock="8" paddingInline="16 8">
        <Heading size="small">Opplysninger A-Inntekt</Heading>
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Måned</Table.HeaderCell>
              <Table.HeaderCell>inntekt</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {opplysninger.ainntekt.inntektsmaaneder.map((maaned, i) => {
              return (
                <Table.Row key={i}>
                  <Table.DataCell>{maaned.maaned}</Table.DataCell>
                  <Table.DataCell>{NOK(maaned.summertBeloep)}</Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
      </VStack>
      <VStack maxWidth="80rem" paddingBlock="8" paddingInline="16 8">
        <AvkortingInntektTabell
          avkortingGrunnlagListe={opplysninger.tidligereAvkorting.avkortingGrunnlag}
          fyller67={false}
        />
      </VStack>
      <VStack paddingBlock="8" paddingInline="16 8">
        <YtelseEtterAvkorting
          avkortetYtelse={opplysninger.tidligereAvkorting.avkortetYtelse}
          tidligereAvkortetYtelse={[]}
        />
      </VStack>
    </>
  )
}
