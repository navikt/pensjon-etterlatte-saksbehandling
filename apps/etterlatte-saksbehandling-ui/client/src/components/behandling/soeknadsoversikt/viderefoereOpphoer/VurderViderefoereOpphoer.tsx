import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaartyper } from '~shared/api/vilkaarsvurdering'
import { mapResult } from '~shared/api/apiUtils'
import { SammendragAvViderefoereOpphoerVurdering } from '~components/behandling/soeknadsoversikt/viderefoereOpphoer/SammendragAvViderefoereOpphoerVurdering'
import { useEffect } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { VurderViderefoereOpphoerSkjema } from '~components/behandling/soeknadsoversikt/viderefoereOpphoer/VurderViderefoereOpphoerSkjema'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  setVurdert: (vurdert: boolean) => void
}

export const VurderViderefoereOpphoer = ({ redigerbar, behandling }: Props) => {
  const [hentVilkaartyperResult, hentVilkaartyperRequest] = useApiCall(hentVilkaartyper)

  const avbryt = () => {}

  const lagre = () => {}

  useEffect(() => {
    hentVilkaartyperRequest(behandling.id)
  }, [])

  return mapResult(hentVilkaartyperResult, {
    pending: <Spinner visible label="Henter vilkårtyper..." />,
    error: <ApiErrorAlert>Kunne ikke hente vilkårtyper</ApiErrorAlert>,
    success: (vilkaartyper) => (
      <VurderingsboksWrapper
        tittel="Skal opphøret umiddelbart videreføres?"
        subtittelKomponent={
          <SammendragAvViderefoereOpphoerVurdering
            viderefoereOpphoer={behandling.viderefoertOpphoer}
            vilkaartyper={vilkaartyper.typer}
          />
        }
        redigerbar={redigerbar}
        vurdering={
          behandling.viderefoertOpphoer?.kilde
            ? {
                saksbehandler: behandling.viderefoertOpphoer?.kilde.ident,
                tidspunkt: new Date(behandling.viderefoertOpphoer?.kilde.tidspunkt),
              }
            : undefined
        }
        kommentar={behandling.viderefoertOpphoer?.begrunnelse}
        defaultRediger={behandling.viderefoertOpphoer === null}
      >
        <VurderViderefoereOpphoerSkjema
          viderefoereOpphoer={behandling.viderefoertOpphoer}
          vilkaartyper={vilkaartyper.typer}
        />
      </VurderingsboksWrapper>
    ),
  })
}
