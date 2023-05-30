import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import React from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'

enum Reduksjon {
  JA_VANLIG = 'Ja, etter vanlig sats(10% av G)',
  NEI_KORT_OPPHOLD = 'Nei, kort opphold',
  JA_EGEN_PROSENT_AV_G = 'Ja, utgifter til bolig(egen % av G)',
  NEI_HOEYE_UTGIFTER_BOLIG = 'Nei, har høye utgifter til bolig',
}

export interface InstitusjonsoppholdIBeregning {
  reduksjon: Reduksjon
  egenReduksjon?: number
  begrunnelse: string
}

export type InstitusjonsoppholdGrunnlag = PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning[]>[]

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlag) => void
}
const Institusjonsopphold = (props: InstitusjonsoppholdProps) => {
  const { behandling } = props
  console.log(behandling.sakType)
  //TODO: hent hendelser på behandling som er av type instittusjonsopphold
  return (
    <>
      <InstitusjonsoppholdsWrapper>
        <LovtekstMedLenke
          tittel={'Institusjonsopphold'}
          hjemler={[
            {
              tittel: '§ 18-8.Barnepensjon under opphold i institusjon',
              lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8',
            },
          ]}
          status={null}
        >
          <p>
            Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig ansvar
            eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske sykehusavdelinger.
            Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at barnepensjonen skal bli redusert.
            Dersom barnet har faste og nødvendige utgifter til bolig, kan arbeids- og velferdsetaten bestemme at
            barnepensjonen ikke skal reduseres eller reduseres mindre enn hovedregelen sier.
          </p>
        </LovtekstMedLenke>
        <Heading level="3" size="small">
          Beregningsperiode institusjonsopphold
        </Heading>
      </InstitusjonsoppholdsWrapper>
    </>
  )
}

export default Institusjonsopphold

const InstitusjonsoppholdsWrapper = styled.div`
  padding: 0em 4em;
  max-width: 56em;
`
