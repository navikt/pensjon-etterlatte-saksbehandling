import { ApiResponse, ApiSuccess } from '~shared/api/apiClient'
import { Tilbakekreving, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { SakType } from '~shared/types/sak'

export function hentTilbakekreving(tilbakekrevingId: string): Promise<ApiResponse<Tilbakekreving>> {
  console.log(tilbakekrevingId)
  // TODO return apiClient.get(`/tilbakekreving/${tilbakekrevingId}`)
  const tilbakekrevingMock: Tilbakekreving = {
    id: '1',
    status: TilbakekrevingStatus.OPPRETTET,
    sak: {
      id: 474,
      ident: '10078201296',
      sakType: SakType.OMSTILLINGSSTOENAD,
      enhet: '4862',
    },
    opprettet: 'timestamp',
    kravgrunnlag: {
      perioder: [
        {
          fra: '2023-01-01',
          til: '2023-01-02',
          beskrivelse: 'Omstillingsstønad',
          bruttoUtbetaling: 1000,
          beregnetNyBrutto: 1200,
          beregnetFeilutbetaling: 200,
          skatteprosent: 20,
          buttoTilbakekreving: 200,
          nettoTilbakekreving: 200,
          skatt: 200,
          resultat: null,
          skyld: null,
          aarsak: null,
        },
        {
          fra: '2023-01-01',
          til: '2023-01-02',
          beskrivelse: 'Feilkonto',
          bruttoUtbetaling: 0,
          beregnetNyBrutto: 0,
          beregnetFeilutbetaling: 0,
          skatteprosent: 0,
          buttoTilbakekreving: 0,
          nettoTilbakekreving: 0,
          skatt: 0,
          resultat: null,
          skyld: null,
          aarsak: null,
        },
      ],
    },
  }

  const apiResponse: ApiSuccess<Tilbakekreving> = { status: 'ok', data: tilbakekrevingMock, statusCode: 200 }
  return Promise.resolve(apiResponse)
}
