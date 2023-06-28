import { BodyShort, Heading, Label } from '@navikt/ds-react'
import { differenceInYears } from 'date-fns'
import styled from 'styled-components'
import { Beregningsperiode, Reduksjon } from '~shared/types/Beregning'
import React from 'react'

interface BeregningsdetaljerPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string | Date
}

export const Barnepensjonberegningssammendrag = ({
  beregningsperiode,
  soesken,
  soeker,
}: {
  beregningsperiode: Beregningsperiode
  soesken: BeregningsdetaljerPerson[] | undefined
  soeker: BeregningsdetaljerPerson
}) => {
  return (
    <>
      {beregningsperiode.soeskenFlokk && (
        <>
          <Heading level="1" size="small">
            Søskenjustering
          </Heading>
          <BodyShort spacing>
            <strong>§18-5</strong> En forelder død: 40% av G til første barn, 25% av G til resterende. Beløpene slås
            sammen og fordeles likt.
          </BodyShort>
          {soesken && (
            <>
              <Label>Beregningen gjelder:</Label>
              <ul>
                {beregningsperiode.soeskenFlokk
                  .map((fnr) => soesken?.find((p) => p.foedselsnummer === fnr))
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
      )}
      {beregningsperiode?.institusjonsopphold && (
        <>
          <HeadingWithTopMargin level="1" size="small">
            Institusjonsopphold
          </HeadingWithTopMargin>
          <div>
            <strong>§18-8</strong> Institusjonsopphold. Barnepensjon kan beregnes ut fra:
            {Object.entries(Reduksjon).map(([reduksjonsnoekkel, reduksjontekst]) => (
              <div key={reduksjonsnoekkel}>{reduksjontekst}</div>
            ))}
            <strong>Beregningen gjelder: </strong>
            <ListWithoutBullet>
              {Reduksjon[beregningsperiode.institusjonsopphold.reduksjon as keyof typeof Reduksjon]}
              {beregningsperiode.institusjonsopphold.egenReduksjon && (
                <p>Egen reduksjon: {beregningsperiode.institusjonsopphold.egenReduksjon}</p>
              )}
              {beregningsperiode.institusjonsopphold.begrunnelse && (
                <p>Begrunnelse: {beregningsperiode.institusjonsopphold.begrunnelse}</p>
              )}
            </ListWithoutBullet>
          </div>
        </>
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
