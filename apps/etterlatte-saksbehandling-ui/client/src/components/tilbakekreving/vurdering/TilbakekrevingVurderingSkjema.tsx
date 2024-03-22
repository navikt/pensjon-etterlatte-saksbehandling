import { Alert, BodyLong, Button, Label, Radio, RadioGroup, Select, Textarea, VStack } from '@navikt/ds-react'
import {
  teksterTilbakekrevingAarsak,
  teksterTilbakekrevingBeloepBehold,
  teksterTilbakekrevingHjemmel,
  teksterTilbakekrevingVarsel,
  teksterTilbakekrevingVilkaar,
  TilbakekrevingAarsak,
  TilbakekrevingBehandling,
  TilbakekrevingBeloepBeholdSvar,
  TilbakekrevingHjemmel,
  TilbakekrevingVarsel,
  TilbakekrevingVilkaar,
  TilbakekrevingVurdering,
} from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { lagreTilbakekrevingsvurdering } from '~shared/api/tilbakekreving'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Toast } from '~shared/alerts/Toast'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { parseISO } from 'date-fns'

export const initialVurdering = {
  aarsak: null,
  beskrivelse: null,
  forhaandsvarsel: null,
  forhaandsvarselDato: null,
  doedsbosak: null,
  foraarsaketAv: null,
  tilsvar: null,
  rettsligGrunnlag: null,
  objektivtVilkaarOppfylt: null,
  subjektivtVilkaarOppfylt: null,
  uaktsomtForaarsaketFeilutbetaling: null,
  burdeBrukerForstaatt: null,
  burdeBrukerForstaattEllerUaktsomtForaarsaket: null,
  vilkaarsresultat: null,
  beloepBehold: null,
  reduseringAvKravet: null,
  foreldet: null,
  rentevurdering: null,
  vedtak: null,
  vurderesForPaatale: null,
  hjemmel: null,
}

export function TilbakekrevingVurderingSkjema({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const dispatch = useAppDispatch()
  const [lagreVurderingStatus, lagreVurderingRequest] = useApiCall(lagreTilbakekrevingsvurdering)
  const [vurdering, setVurdering] = useState<TilbakekrevingVurdering>(
    behandling.tilbakekreving.vurdering ? behandling.tilbakekreving.vurdering : initialVurdering
  )

  const lagreVurdering = () => {
    // TODO validering?
    lagreVurderingRequest({ behandlingsId: behandling.id, vurdering }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  const vilkaarOppfylt = (vilkaarsresultat: TilbakekrevingVilkaar) =>
    [TilbakekrevingVilkaar.OPPFYLT, TilbakekrevingVilkaar.DELVIS_OPPFYLT].includes(vilkaarsresultat)

  const vilkaarOppfyltEllerBeloepIBehold = (vurdering: TilbakekrevingVurdering) =>
    (vurdering.vilkaarsresultat && vilkaarOppfylt(vurdering.vilkaarsresultat)) ||
    vurdering.beloepBehold?.behold == TilbakekrevingBeloepBeholdSvar.BELOEP_I_BEHOLD

  const hjemmelFraVurdering = (
    rettsligGrunnlag: TilbakekrevingHjemmel | null,
    vilkaarsresultat: TilbakekrevingVilkaar | null
  ): TilbakekrevingHjemmel | null => {
    // Femte ledd dersom vilkår ikke er oppfylt
    if (vilkaarsresultat === TilbakekrevingVilkaar.IKKE_OPPFYLT) {
      return TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD
    }

    // Ellers det som settes i rettslig grunnlag
    return rettsligGrunnlag
  }

  return (
    <InnholdPadding>
      <VStack gap="8" style={{ width: '50em' }}>
        <Select
          label="Årsak til sak om feilutbetaling"
          readOnly={!redigerbar}
          value={vurdering.aarsak ?? ''}
          onChange={(e) => {
            if (e.target.value == '') return
            setVurdering({
              ...vurdering,
              aarsak: TilbakekrevingAarsak[e.target.value as TilbakekrevingAarsak],
            })
          }}
        >
          <option value="">Velg..</option>
          {Object.values(TilbakekrevingAarsak).map((aarsak) => (
            <option key={aarsak} value={aarsak}>
              {teksterTilbakekrevingAarsak[aarsak]}
            </option>
          ))}
        </Select>

        <RadioGroup
          legend={<div style={{ fontSize: 'large' }}>Forhåndsvarsel</div>}
          readOnly={!redigerbar}
          size="small"
          className="radioGroup"
          value={vurdering.forhaandsvarsel ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              forhaandsvarsel: TilbakekrevingVarsel[e as TilbakekrevingVarsel],
            })
          }
        >
          <div className="flex">
            {Object.values(TilbakekrevingVarsel).map((varsel) => (
              <Radio key={varsel} value={varsel}>
                {teksterTilbakekrevingVarsel[varsel]}
              </Radio>
            ))}
          </div>
        </RadioGroup>

        <DatoVelger
          value={vurdering?.forhaandsvarselDato ? parseISO(vurdering.forhaandsvarselDato) : undefined}
          readOnly={!redigerbar}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              forhaandsvarselDato: e?.toISOString() ?? null,
            })
          }
          label="Forhåndsvarsel dato"
        ></DatoVelger>

        <Textarea
          label="Beskriv feilutbetalingen"
          readOnly={!redigerbar}
          description="Gi en kort beskrivelse av bakgrunnen for feilutbetalingen og når ble den oppdaget."
          value={vurdering.beskrivelse ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              beskrivelse: e.target.value,
            })
          }
        />

        <RadioGroup
          legend={<div style={{ fontSize: 'large' }}>Dødsbosak?</div>}
          readOnly={!redigerbar}
          size="small"
          className="radioGroup"
          value={vurdering.doedsbosak ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              doedsbosak: JaNei[e as JaNei],
            })
          }
        >
          <div className="flex">
            {Object.values(JaNei).map((svar) => (
              <Radio key={svar} value={svar}>
                {JaNeiRec[svar]}
              </Radio>
            ))}
          </div>
        </RadioGroup>

        <Textarea
          label="Hvem forårsaket feilutbetalingen?"
          readOnly={!redigerbar}
          value={vurdering.foraarsaketAv ?? ''}
          onChange={(e) =>
            setVurdering({
              ...vurdering,
              foraarsaketAv: e.target.value,
            })
          }
        />

        <RadioGroup
          legend={<div style={{ fontSize: 'large' }}>Tilsvar til varsel om mulig tilbakekreving?</div>}
          readOnly={!redigerbar}
          size="small"
          className="radioGroup"
          value={vurdering.tilsvar?.tilsvar ?? ''}
          onChange={(e) => {
            const tilsvar = e as JaNei
            setVurdering({
              ...vurdering,
              tilsvar: {
                ...vurdering.tilsvar,
                tilsvar: JaNei[tilsvar],
                beskrivelse: tilsvar === JaNei.NEI ? null : vurdering.tilsvar?.beskrivelse ?? null,
                dato: tilsvar === JaNei.NEI ? null : vurdering.tilsvar?.dato ?? null,
              },
            })
          }}
        >
          <div className="flex">
            {Object.values(JaNei).map((svar) => (
              <Radio key={svar} value={svar}>
                {JaNeiRec[svar]}
              </Radio>
            ))}
          </div>
        </RadioGroup>

        {vurdering.tilsvar?.tilsvar == JaNei.JA && (
          <>
            <DatoVelger
              value={vurdering.tilsvar?.dato ? parseISO(vurdering.tilsvar?.dato) : undefined}
              readOnly={!redigerbar}
              onChange={(e) =>
                setVurdering({
                  ...vurdering,
                  tilsvar: {
                    ...vurdering.tilsvar,
                    beskrivelse: vurdering.tilsvar?.beskrivelse ?? null,
                    tilsvar: vurdering.tilsvar?.tilsvar ?? null,
                    dato: e?.toISOString() ?? null,
                  },
                })
              }
              label="Tilsvar dato"
            ></DatoVelger>

            <Textarea
              label="Beskriv tilsvar"
              readOnly={!redigerbar}
              value={vurdering.tilsvar?.beskrivelse ?? ''}
              onChange={(e) =>
                setVurdering({
                  ...vurdering,
                  tilsvar: {
                    ...vurdering.tilsvar,
                    tilsvar: vurdering.tilsvar?.tilsvar ?? null,
                    dato: vurdering.tilsvar?.dato ?? null,
                    beskrivelse: e.target.value,
                  },
                })
              }
            />
          </>
        )}

        <RadioGroup
          legend={<div style={{ fontSize: 'large' }}>Rettslig grunnlag</div>}
          readOnly={!redigerbar}
          size="small"
          className="radioGroup"
          value={vurdering.rettsligGrunnlag ?? ''}
          onChange={(e) => {
            setVurdering({
              ...vurdering,
              rettsligGrunnlag: TilbakekrevingHjemmel[e as TilbakekrevingHjemmel],
              hjemmel: hjemmelFraVurdering(
                TilbakekrevingHjemmel[e as TilbakekrevingHjemmel],
                vurdering.vilkaarsresultat
              ),
              reduseringAvKravet: null,
              foreldet: null,
              rentevurdering: null,
              vurderesForPaatale: null,
              beloepBehold: null,
              objektivtVilkaarOppfylt: null,
              subjektivtVilkaarOppfylt: null,
              burdeBrukerForstaatt: null,
              uaktsomtForaarsaketFeilutbetaling: null,
              burdeBrukerForstaattEllerUaktsomtForaarsaket: null,
              vilkaarsresultat: null,
              vedtak: null,
            })
          }}
        >
          <div className="flex">
            {Object.values(TilbakekrevingHjemmel)
              .filter((hjemmel) => hjemmel != TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD)
              .map((hjemmel) => (
                <Radio key={hjemmel} value={hjemmel}>
                  {teksterTilbakekrevingHjemmel[hjemmel]}
                </Radio>
              ))}
          </div>
        </RadioGroup>

        {vurdering.rettsligGrunnlag &&
          [
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM,
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM,
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM,
          ].includes(vurdering.rettsligGrunnlag) && (
            <>
              <Textarea
                label="Er det objektive vilkåret oppfylt?"
                description="Foreligger det en feilutbetaling?"
                readOnly={!redigerbar}
                value={vurdering.objektivtVilkaarOppfylt ?? ''}
                onChange={(e) =>
                  setVurdering({
                    ...vurdering,
                    objektivtVilkaarOppfylt: e.target.value,
                  })
                }
              />

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM].includes(
                vurdering.rettsligGrunnlag
              ) && (
                <Textarea
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Forstod eller burde brukeren forstått at ubetalingen skyldes en feil?"
                  readOnly={!redigerbar}
                  value={vurdering.burdeBrukerForstaatt ?? ''}
                  onChange={(e) =>
                    setVurdering({
                      ...vurdering,
                      burdeBrukerForstaatt: e.target.value,
                    })
                  }
                />
              )}

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM].includes(
                vurdering.rettsligGrunnlag
              ) && (
                <Textarea
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Har brukeren uaktsomt forårsaket feilutbetalingen?"
                  readOnly={!redigerbar}
                  value={vurdering.uaktsomtForaarsaketFeilutbetaling ?? ''}
                  onChange={(e) =>
                    setVurdering({
                      ...vurdering,
                      uaktsomtForaarsaketFeilutbetaling: e.target.value,
                    })
                  }
                />
              )}

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM].includes(
                vurdering.rettsligGrunnlag
              ) && (
                <Textarea
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Forstod eller burde brukeren forstått at utbetalingen skyldtes en feil, og/eller har brukeren uaktsomt forårsaket feilutbetalingen?"
                  readOnly={!redigerbar}
                  value={vurdering.burdeBrukerForstaattEllerUaktsomtForaarsaket ?? ''}
                  onChange={(e) =>
                    setVurdering({
                      ...vurdering,
                      burdeBrukerForstaattEllerUaktsomtForaarsaket: e.target.value,
                    })
                  }
                />
              )}

              <RadioGroup
                legend=""
                hideLegend={true}
                readOnly={!redigerbar}
                size="small"
                className="radioGroup"
                value={vurdering.vilkaarsresultat ?? ''}
                onChange={(e) => {
                  const vilkaarsresultat = e as TilbakekrevingVilkaar
                  setVurdering({
                    ...vurdering,
                    vilkaarsresultat: TilbakekrevingVilkaar[vilkaarsresultat],
                    hjemmel: hjemmelFraVurdering(vurdering.rettsligGrunnlag, TilbakekrevingVilkaar[vilkaarsresultat]),
                    beloepBehold: null,
                  })
                }}
              >
                <div className="flex">
                  {Object.values(TilbakekrevingVilkaar).map((vilkaar) => (
                    <Radio key={vilkaar} value={vilkaar}>
                      {teksterTilbakekrevingVilkaar[vilkaar]}
                    </Radio>
                  ))}
                </div>
              </RadioGroup>

              {vurdering.vilkaarsresultat && vurdering.vilkaarsresultat == TilbakekrevingVilkaar.IKKE_OPPFYLT && (
                <>
                  <Textarea
                    label="Tilbakekreving etter folketrygdloven § 22-15 femte ledd?"
                    description="Er noe av det feilutbetalte beløpet i behold?"
                    readOnly={!redigerbar}
                    value={vurdering.beloepBehold?.beskrivelse ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        beloepBehold: {
                          ...vurdering.beloepBehold,
                          behold: vurdering.beloepBehold?.behold ?? null,
                          beskrivelse: e.target.value,
                        },
                      })
                    }
                  />

                  <RadioGroup
                    legend=""
                    hideLegend={true}
                    readOnly={!redigerbar}
                    size="small"
                    className="radioGroup"
                    value={vurdering.beloepBehold?.behold ?? ''}
                    onChange={(e) => {
                      const beloepBeholdSvar = e as TilbakekrevingBeloepBeholdSvar
                      const beloepBehold = beloepBeholdSvar === TilbakekrevingBeloepBeholdSvar.BELOEP_I_BEHOLD
                      setVurdering({
                        ...vurdering,
                        beloepBehold: {
                          ...vurdering.beloepBehold,
                          behold: TilbakekrevingBeloepBeholdSvar[beloepBeholdSvar],
                          beskrivelse: vurdering.beloepBehold?.beskrivelse ?? null,
                        },
                        reduseringAvKravet: beloepBehold ? vurdering.reduseringAvKravet : null,
                        foreldet: beloepBehold ? vurdering.foreldet : null,
                        rentevurdering: beloepBehold ? vurdering.rentevurdering : null,
                        vurderesForPaatale: beloepBehold ? vurdering.vurderesForPaatale : null,
                      })
                    }}
                  >
                    <div className="flex">
                      {Object.values(TilbakekrevingBeloepBeholdSvar).map((behold) => (
                        <Radio key={behold} value={behold}>
                          {teksterTilbakekrevingBeloepBehold[behold]}
                        </Radio>
                      ))}
                    </div>
                  </RadioGroup>

                  {vurdering.beloepBehold?.behold == TilbakekrevingBeloepBeholdSvar.BELOEP_IKKE_I_BEHOLD && (
                    <Textarea
                      label="Vedtak"
                      readOnly={!redigerbar}
                      value={vurdering.vedtak ?? ''}
                      onChange={(e) =>
                        setVurdering({
                          ...vurdering,
                          vedtak: e.target.value,
                        })
                      }
                    />
                  )}
                </>
              )}
              {vilkaarOppfyltEllerBeloepIBehold(vurdering) && (
                <>
                  <Textarea
                    label="Er det særlige grunner til å frafalle eller redusere kravet?"
                    description="Det legges blant annet vekt på graden av uaktsomhet hos brukeren, størrelsen på det feilutbetalte beløpet, hvor lang tid det er gått siden utbetalingen fant sted og om noe av feilen helt eller delvis kan tilskrives NAV. Kravet kan frafalles helt, eller settes til en del av det feilutbetalte beløpet. Ved utvist forsett skal krav alltid fremmes, og beløpet kan ikke settes ned."
                    readOnly={!redigerbar}
                    value={vurdering.reduseringAvKravet ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        reduseringAvKravet: e.target.value,
                      })
                    }
                  />

                  <Textarea
                    label="Er noen deler av kravet foreldet?"
                    description="Det er bestemt i folketrygdloven § 22-14 første ledd at våre krav om tilbakebetaling i utgangspunktet foreldes etter foreldelsesloven. Etter foreldelsesloven § 2, jf. § 3 nr. 1 er den alminnelige foreldelsesfristen tre år. Fristen løper særskilt for hver månedsutbetaling. Vurder også om foreldelsesloven § 10 om ett års tilleggsfrist får anvendelse."
                    readOnly={!redigerbar}
                    value={vurdering.foreldet ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        foreldet: e.target.value,
                      })
                    }
                  />

                  <Textarea
                    label="Skal det ilegges renter?"
                    description="Det følger av folketrygdloven § 22-17 a at det skal beregnes et rentetillegg på 10 prosent av det beløpet som kreves tilbake når brukeren har opptrådt grovt uaktsomt eller med forsett."
                    readOnly={!redigerbar}
                    value={vurdering.rentevurdering ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        rentevurdering: e.target.value,
                      })
                    }
                  />

                  <Textarea
                    label="Vedtak"
                    readOnly={!redigerbar}
                    value={vurdering.vedtak ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        vedtak: e.target.value,
                      })
                    }
                  />

                  <Textarea
                    label="Skal saken vurderes for påtale?"
                    readOnly={!redigerbar}
                    value={vurdering.vurderesForPaatale ?? ''}
                    onChange={(e) =>
                      setVurdering({
                        ...vurdering,
                        vurderesForPaatale: e.target.value,
                      })
                    }
                  />
                </>
              )}
            </>
          )}

        <div>
          <Label>Hjemmel</Label>
          <BodyLong>{vurdering.hjemmel ? teksterTilbakekrevingHjemmel[vurdering.hjemmel] : 'Ikke satt'}</BodyLong>
        </div>

        {redigerbar && (
          <VStack gap="5">
            <Button
              variant="primary"
              size="small"
              onClick={lagreVurdering}
              loading={isPending(lagreVurderingStatus)}
              style={{ maxWidth: '7.5em' }}
            >
              Lagre vurdering
            </Button>
            {mapResult(lagreVurderingStatus, {
              success: () => <Toast melding="Vurdering lagret" />,
              error: (error) => <Alert variant="error">Kunne ikke lagre vurdering: {error.detail}</Alert>,
            })}
          </VStack>
        )}
      </VStack>
    </InnholdPadding>
  )
}
