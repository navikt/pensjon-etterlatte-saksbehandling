import { BodyShort, Heading, Label } from '@navikt/ds-react'
import { differenceInYears, isAfter, parseISO } from 'date-fns'
import styled from 'styled-components'
import {
  Beregningsperiode,
  InstitusjonsoppholdIBeregning,
  OverstyrBeregning,
  ReduksjonBP,
} from '~shared/types/Beregning'
import React from 'react'

interface BeregningsdetaljerPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string | Date
}

const SISTE_MAANED_GAMMELT_REGELVERK = new Date(2023, 11, 31, 0, 0, 0, 0)

const SammendragGammeltRegelverk = (props: {
  soesken: BeregningsdetaljerPerson[]
  soeker: BeregningsdetaljerPerson
  beregningsperiode: Beregningsperiode
}) => {
  const { soesken, soeker, beregningsperiode } = props

  return (
    <>
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
                      } / ${differenceInYears(new Date(), new Date(soeskenIFlokken.foedselsdato))} år`}
                    </ListWithoutBullet>
                  )
                )
              })}
          </ul>
        </>
      )}
    </>
  )
}

const SammendragNyttRegelverk = (props: { soeker: BeregningsdetaljerPerson }) => {
  const { soeker } = props

  return (
    <>
      <Heading level="3" size="small">
        Beregning av barnepensjon
      </Heading>
      <BodyShort spacing>
        <strong>§18-5</strong> En forelder død: 100% av G til barnet.
      </BodyShort>

      <Label>Beregningen gjelder:</Label>
      <ul>
        <ListWithoutBullet>
          {`${soeker.fornavn} ${soeker.etternavn} / ${soeker.foedselsnummer} / ${differenceInYears(
            new Date(),
            new Date(soeker.foedselsdato)
          )} år`}
        </ListWithoutBullet>
      </ul>
    </>
  )
}

const SammendragInstitusjonsopphold = (props: { institusjonsopphold: InstitusjonsoppholdIBeregning }) => {
  const { institusjonsopphold } = props

  return (
    <>
      <HeadingWithTopMargin level="3" size="small">
        Institusjonsopphold
      </HeadingWithTopMargin>
      <div>
        <strong>§18-8</strong> En forelder død: Barnepensjonen reduseres til 10 % av G ved lengre institusjonsopphold.
        Hvis man har utgifter til bolig kan man likevel slippe reduksjon eller få en lavere reduksjon.
        <div>
          <Label>Beregningen gjelder: </Label>
          <ListWithoutBullet>
            {ReduksjonBP[institusjonsopphold.reduksjon]}
            {institusjonsopphold.egenReduksjon && <p>Egen reduksjon: {institusjonsopphold.egenReduksjon}</p>}
          </ListWithoutBullet>
        </div>
      </div>
    </>
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
  const datoPeriodeFom = parseISO(beregningsperiode.datoFOM)
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

  const erPaaNyttRegelverk = isAfter(datoPeriodeFom, SISTE_MAANED_GAMMELT_REGELVERK)

  console.log('datoer', datoPeriodeFom, erPaaNyttRegelverk)
  return (
    <>
      {erPaaNyttRegelverk ? (
        <SammendragNyttRegelverk soeker={soeker} />
      ) : (
        <SammendragGammeltRegelverk soesken={soesken ?? []} soeker={soeker} beregningsperiode={beregningsperiode} />
      )}
      {beregningsperiode?.institusjonsopphold && (
        <SammendragInstitusjonsopphold institusjonsopphold={beregningsperiode.institusjonsopphold} />
      )}
    </>
  )
}

const HeadingWithTopMargin = styled(Heading)`
  margin-top: 1em;
`

const ListWithoutBullet = styled.li`
  list-style-type: none;
`
