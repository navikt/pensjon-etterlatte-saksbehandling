import { BodyShort, Heading, Label } from '@navikt/ds-react'
import { differenceInYears } from 'date-fns'
import styled from 'styled-components'
import { BeregningsGrunnlagPostDto, Beregningsperiode } from '~shared/types/Beregning'
import { useEffect, useState } from 'react'
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
  const soeskenFunnet = beregningsperiode.soeskenFlokk.map((fnr) => {
    const temp = soesken?.find((p) => p.foedselsnummer === fnr)
    return temp
  })
  const soeskenFlokk = [...soeskenFunnet, soeker]

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
              {soesken && soeskenFunnet && soeskenFlokk && (
                <>
                  <Label>Beregningen gjelder:</Label>
                  <ul>
                    {soeskenFlokk.map((soeskenIFlokken) => (
                      <ListWithoutBullet key={soeskenIFlokken.foedselsnummer}>
                        {`${soeskenIFlokken.fornavn} ${soeskenIFlokken.etternavn} / ${
                          soeskenIFlokken.foedselsnummer
                        } / ${differenceInYears(new Date(), new Date(soeskenIFlokken.foedselsdato))} år`}
                      </ListWithoutBullet>
                    ))}
                  </ul>
                </>
              )}
            </>
          )}
          {beregningsgrunnlagSate?.institusjonsopphold && (
            <BodyShort spacing>
              <strong>§18-8</strong> blablalba institusjon
            </BodyShort>
          )}
        </>
      )}
    </>
  )
}

const ListWithoutBullet = styled.li`
  list-style-type: none;
`
