import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { isAfter, parseISO } from 'date-fns'
import styled from 'styled-components'
import {
  Beregningsperiode,
  InstitusjonsoppholdIBeregning,
  OverstyrBeregning,
  ReduksjonBP,
} from '~shared/types/Beregning'
import React from 'react'
import { hentAlderForDato } from '~components/behandling/felles/utils'

interface BeregningsdetaljerPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: Date
}

export const SISTE_DATO_GAMMELT_REGELVERK = new Date(2023, 11, 31, 0, 0, 0, 0)

const SammendragGammeltRegelverk = (props: {
  soesken: BeregningsdetaljerPerson[]
  soeker: BeregningsdetaljerPerson
  beregningsperiode: Beregningsperiode
}) => {
  const { soesken, soeker, beregningsperiode } = props

  return (
    <div>
      <Heading level="3" size="small">
        Søskenjustering
      </Heading>
      <BodyShort spacing>
        <strong>§18-5</strong> En forelder død: 40% av G til første barn, 25% av G til resterende. Beløpene slås sammen
        og fordeles likt.
      </BodyShort>
      {soesken && (
        <>
          <Label>Beregningen gjelder:</Label>
          <ul>
            {beregningsperiode.soeskenFlokk
              .map((fnr) => soesken.find((p) => p.foedselsnummer === fnr))
              .concat([soeker])
              .map((soeskenIFlokken) => {
                return (
                  soeskenIFlokken && (
                    <ListWithoutBullet key={soeskenIFlokken.foedselsnummer}>
                      {`${soeskenIFlokken.fornavn} ${soeskenIFlokken.etternavn} / ${
                        soeskenIFlokken.foedselsnummer
                      } / ${hentAlderForDato(soeskenIFlokken.foedselsdato)} år`}
                    </ListWithoutBullet>
                  )
                )
              })}
          </ul>
        </>
      )}
    </div>
  )
}

const SammendragNyttRegelverk = (props: {
  soeker: BeregningsdetaljerPerson
  flereAvdoede: boolean
  kunEnJuridiskForelder: boolean
}) => {
  const { soeker, flereAvdoede, kunEnJuridiskForelder } = props

  const tekst = flereAvdoede
    ? 'To foreldre døde: 2,25 x G til barnet.'
    : kunEnJuridiskForelder
      ? 'Kun én juridisk forelder: 2,25 x G til barnet.'
      : 'En forelder død: 100% av G til barnet.'

  return (
    <div>
      <Heading level="3" size="small">
        Beregning av barnepensjon
      </Heading>
      <BodyShort spacing>
        <strong>§18-5</strong> {tekst}
      </BodyShort>

      <Label>Beregningen gjelder:</Label>
      <ul>
        <ListWithoutBullet>
          {soeker.fornavn} {soeker.etternavn} / {soeker.foedselsnummer} {hentAlderForDato(soeker.foedselsdato)} år
        </ListWithoutBullet>
      </ul>
    </div>
  )
}

const SammendragInstitusjonsopphold = (props: { institusjonsopphold: InstitusjonsoppholdIBeregning }) => {
  const { institusjonsopphold } = props

  return (
    <div>
      <Heading level="3" size="small">
        Institusjonsopphold
      </Heading>
      <BodyShort spacing>
        <strong>§18-8</strong> En forelder død: Barnepensjonen reduseres til 10 % av G ved lengre institusjonsopphold.
        Hvis man har utgifter til bolig kan man likevel slippe reduksjon eller få en lavere reduksjon.
      </BodyShort>
      <div>
        <Label>Beregningen gjelder: </Label>
        <ListWithoutBullet>
          {ReduksjonBP[institusjonsopphold.reduksjon]}
          {institusjonsopphold.egenReduksjon && <p>Egen reduksjon: {institusjonsopphold.egenReduksjon}</p>}
        </ListWithoutBullet>
      </div>
    </div>
  )
}

export const Barnepensjonberegningssammendrag = ({
  beregningsperiode,
  overstyring,
  soesken,
  soeker,
}: {
  beregningsperiode: Beregningsperiode
  overstyring?: OverstyrBeregning
  soesken: BeregningsdetaljerPerson[] | undefined
  soeker: BeregningsdetaljerPerson
}) => {
  if (overstyring) {
    return (
      <>
        <Heading level="3" size="small">
          Overstyrt beregning
        </Heading>
        <BodyShort>Beregningen er manuelt overstyrt, på grunn av {overstyring.beskrivelse}.</BodyShort>
      </>
    )
  }

  const datoPeriodeFom = parseISO(beregningsperiode.datoFOM)
  const erPaaNyttRegelverk = isAfter(datoPeriodeFom, SISTE_DATO_GAMMELT_REGELVERK)

  const flereAvdoede = (beregningsperiode.avdoedeForeldre ?? []).length > 1
  const kunEnJuridiskForelder = beregningsperiode.kunEnJuridiskForelder

  return (
    <VStack gap="space-4">
      {erPaaNyttRegelverk ? (
        <SammendragNyttRegelverk
          soeker={soeker}
          flereAvdoede={flereAvdoede}
          kunEnJuridiskForelder={kunEnJuridiskForelder}
        />
      ) : (
        <SammendragGammeltRegelverk soesken={soesken ?? []} soeker={soeker} beregningsperiode={beregningsperiode} />
      )}
      {beregningsperiode?.institusjonsopphold && (
        <SammendragInstitusjonsopphold institusjonsopphold={beregningsperiode.institusjonsopphold} />
      )}
    </VStack>
  )
}

const ListWithoutBullet = styled.li`
  list-style-type: none;
`
