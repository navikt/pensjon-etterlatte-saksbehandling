import React from 'react'
import styled from 'styled-components'
import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { BodyShort, Box, ReadMore } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { tekstSanksjon } from '~shared/types/sanksjon'

export const YtelseEtterAvkortingDetaljer = (props: { ytelse: IAvkortetYtelse }) => {
  const ytelse = props.ytelse

  return (
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
      <Wrapper>
        <ul>
          <Rad>
            <Navn>Brutto stønad før avkorting</Navn>
            <Verdi>{NOK(ytelse.ytelseFoerAvkorting)}</Verdi>
          </Rad>
          <Rad>
            <Navn>Månedlig avkortingsbeløp</Navn>
            <Operasjon>-</Operasjon>
            <Verdi>{NOK(ytelse.avkortingsbeloep)}</Verdi>
          </Rad>
          {ytelse.sanksjon ? (
            <Rad>
              <Navn>{tekstSanksjon[ytelse.sanksjon.sanksjonType]} i perioden</Navn>
              <Operasjon>*</Operasjon>
              <Verdi>0</Verdi>
            </Rad>
          ) : ytelse.restanse < 0 ? (
            <Rad>
              <Navn>Månedlig restansebeløp</Navn>
              <Operasjon>+</Operasjon>
              <Verdi>{NOK(ytelse.restanse * -1)}</Verdi>
            </Rad>
          ) : (
            <Rad>
              <Navn>Månedlig restansebeløp</Navn>
              <Operasjon>-</Operasjon>
              <Verdi>{NOK(ytelse.restanse)}</Verdi>
            </Rad>
          )}

          <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
            <Rad>
              {ytelse.sanksjon ? (
                <Navn>Brutto stønad etter sanksjon</Navn>
              ) : (
                <Navn>Brutto stønad etter avkorting og restanse</Navn>
              )}
              <Operasjon>=</Operasjon>
              <Verdi>
                <strong>{NOK(ytelse.ytelseEtterAvkorting)}</strong>
              </Verdi>
            </Rad>
          </Box>
        </ul>
        <Beskrivelse>
          <li>
            <ReadMore header="Hvordan fungerer avkorting av en omstillingstønad?">
              <BodyShort size="small">
                Omstillingsstønaden avkortes med mottakers forventede årsinntekt/12. Er forventet inntekt satt for
                lavt/høyt, og en eventuell restanse (se under) ikke klarer å hente inn igjen for mye/lite utbetalt
                stønad, vil dette bli behandlet i et etteroppgjør.
              </BodyShort>
            </ReadMore>
          </li>
          <li>
            <ReadMore header="Hva er restanse?">
              <BodyShort spacing size="small">
                Hvis man fastsetter en ny forventet inntekt for inneværende år, oppstår det en
                feilutbetaling/etterbetaling, en restanse. Restansen skal fordeles ut over resterende utbetalingsmåneder
                for inneværende år. Dette gjøres for å minimere etteroppgjøret.
              </BodyShort>
              <BodyShort size="small">
                Det er antall gjenstående måneder som avgjør hvor stort restansebeløpet blir. Har man f.eks. endret
                forventet inntekt fra 01.07.2023 og det foreligger en feilutbetaling på 15 000 kroner, skal beløpet
                fordeles på 6 måneder, og restansebeløpet blir 2 500 kr.
              </BodyShort>
            </ReadMore>
          </li>
        </Beskrivelse>
      </Wrapper>
    </Box>
  )
}

const Wrapper = styled.div`
  margin-top: 2em;
  display: flex;

  font-size: 0.9em;
`
const Rad = styled.li`
  margin-bottom: 1.25em;
  overflow: hidden;
`
const Navn = styled.div`
  float: left;
  width: 18em;
`
const Operasjon = styled.div`
  float: left;
  width: 5em;
`
const Verdi = styled.div`
  float: right;
  width: 5em;
`
const Beskrivelse = styled.div`
  flex-grow: 1;
  margin-left: 1em;
  list-style-type: none;
  width: 10em;

  h4 {
    margin: 0;
  }

  li {
    margin-bottom: 2em;
  }
`
