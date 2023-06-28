import { BodyShort, Heading, Label } from '@navikt/ds-react'
import { differenceInYears } from 'date-fns'
import styled from 'styled-components'
import { BeregningsGrunnlagPostDto, Beregningsperiode, Reduksjon } from '~shared/types/Beregning'
import React, { useEffect, useState } from 'react'
import { isFailure, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentBeregningsGrunnlag } from '~shared/api/beregning'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

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
  behandlingId,
}: {
  beregningsperiode: Beregningsperiode
  soesken: BeregningsdetaljerPerson[] | undefined
  soeker: BeregningsdetaljerPerson
  behandlingId: string
}) => {
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const [beregningsgrunnlagSate, setBeregningsgrunnlag] = useState<BeregningsGrunnlagPostDto>()

  useEffect(() => {
    fetchBeregningsgrunnlag(behandlingId, (result) => {
      if (result) {
        setBeregningsgrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
      }
    })
  }, [])

  return (
    <>
      {isPending(beregningsgrunnlag) && <Spinner visible={isPendingOrInitial(beregningsgrunnlag)} label="Laster" />}
      {isFailure(beregningsgrunnlag) && <ApiErrorAlert>Kunne ikke hente beregningsgrunnlag</ApiErrorAlert>}
      {isSuccess(beregningsgrunnlag) && (
        <>
          {beregningsgrunnlagSate?.soeskenMedIBeregning && (
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
          {beregningsgrunnlagSate?.institusjonsopphold?.length ? (
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
                {beregningsgrunnlagSate.institusjonsopphold.map((it) => {
                  return (
                    <ListWithoutBullet key={`${it.fom}${it.data.reduksjon}`}>
                      {Reduksjon[it.data.reduksjon as keyof typeof Reduksjon]}
                      {it.data.egenReduksjon && <p>{it.data.egenReduksjon}</p>}
                    </ListWithoutBullet>
                  )
                })}
              </div>
            </>
          ) : null}
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
