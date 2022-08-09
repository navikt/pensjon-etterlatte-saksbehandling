import { BodyShort, Button, Heading, Radio, RadioGroup } from '@navikt/ds-react'
import { Content, Header } from '../../../shared/styled'
import { useContext, useState } from 'react'
import { Border } from '../soeknadsoversikt/styled'
import { Barn } from '../soeknadsoversikt/familieforhold/personer/Barn'
import { Soesken } from '../soeknadsoversikt/familieforhold/personer/Soesken'
import { AppContext } from '../../../store/AppContext'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { lagreSoeskenMedIBeregning } from '../../../shared/api/behandling'
import { useBehandlingRoutes } from '../BehandlingRoutes'

const Beregningsgrunnlag = () => {
  const behandling = useContext(AppContext).state.behandlingReducer
  const { next } = useBehandlingRoutes()

  if (behandling.kommerSoekerTilgode == null || behandling.familieforhold?.avdoede == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const doedsdato = behandling.kommerSoekerTilgode.familieforhold.avdoed.doedsdato
  const [soeskenMedIBeregning, setSoeskenMedIBeregnin] = useState<
    Array<{ foedselsnummer: string; skalBrukes: boolean }>
  >(
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn!.map((soesken) => ({
      foedselsnummer: soesken.foedselsnummer,
      skalBrukes: false,
    }))
  )

  const handleChange = (fnr: string, skalBrukes: boolean) => {
    const index = soeskenMedIBeregning.findIndex((soesken) => soesken.foedselsnummer === fnr)
    const oppdatertBeregningsgrunnlag = soeskenMedIBeregning.map((soesken) =>
      soesken.foedselsnummer === fnr ? { ...soesken, skalBrukes } : { ...soesken }
    )

    setSoeskenMedIBeregnin(oppdatertBeregningsgrunnlag)
  }

  return (
    <Content>
      <Header>
        <h1>Beregningsgrunnlag</h1>
        <BodyShort spacing>
          Vilkårsresultat: <strong>Innvilget fra 01.06.2019</strong>
        </BodyShort>
        <Heading level="2" size="small">
          Søskenjustering
        </Heading>
      </Header>

      <FamilieforholdWrapper>
        <Barn person={behandling.kommerSoekerTilgode.familieforhold.soeker} doedsdato={doedsdato} />
        <Border />
        {behandling.familieforhold?.avdoede.opplysning.avdoedesBarn?.map((barn) => (
          <div key={barn.fornavn} style={{ display: 'flex', alignItems: 'center' }}>
            <Soesken person={barn} familieforhold={behandling.familieforhold!} />
            <div>
              <RadioGroupRow
                legend="Oppdras sammen"
                onChange={(value: boolean) => handleChange(barn.foedselsnummer, value)}
              >
                <Radio value={true}>Ja</Radio>
                <Radio value={false}>Nei</Radio>
              </RadioGroupRow>
            </div>
          </div>
        ))}
      </FamilieforholdWrapper>
      <BehandlingHandlingKnapper>
        <Button
          variant="primary"
          size="medium"
          onClick={async () => {
            await lagreSoeskenMedIBeregning(behandling.id, soeskenMedIBeregning)
            next()
          }}
        >
          Beregne og fatte vedtak
        </Button>
      </BehandlingHandlingKnapper>
    </Content>
  )
}

const RadioGroupRow = styled(RadioGroup)`
  .navds-radio-buttons {
    display: flex;
    flex-direction: row;
    gap: 12px;
  }
`
const FamilieforholdWrapper = styled.div`
  margin-left: 3em;
`

export default Beregningsgrunnlag
