import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { InfobokserWrapper, InfoWrapper, SoeknadOversiktWrapper, VurderingsWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { Heading } from "@navikt/ds-react";

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
}

export const OversiktKommerBarnetTilgode: React.FC<Props> = ({ kommerBarnetTilgode }) => (
  <>
    <Heading size={"medium"} level={"2"}>Vurdering om pensjonen kommer barnet til gode</Heading>
    <SoeknadOversiktWrapper>
      <InfobokserWrapper>
        <InfoWrapper>Har barnet samme adresse som den gjenlevende forelder?</InfoWrapper>
      </InfobokserWrapper>
      <VurderingsWrapper>
        <KommerBarnetTilGodeVurdering kommerBarnetTilgode={kommerBarnetTilgode} />
      </VurderingsWrapper>
    </SoeknadOversiktWrapper>
  </>
)
