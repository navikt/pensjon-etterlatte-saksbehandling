import { Alert, BodyLong, Button, Label, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
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
import React, { useEffect } from 'react'
import { lagreTilbakekrevingsvurdering } from '~shared/api/tilbakekreving'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Toast } from '~shared/alerts/Toast'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

const initialVurdering: TilbakekrevingVurdering = {
  aarsak: null,
  beskrivelse: null,
  forhaandsvarsel: null,
  forhaandsvarselDato: null,
  doedsbosak: null,
  foraarsaketAv: null,
  tilsvar: null,
  rettsligGrunnlag: null,
  objektivtVilkaarOppfylt: null,
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
  if (!behandling) {
    return
  }
  const [lagreVurderingStatus, lagreVurderingRequest] = useApiCall(lagreTilbakekrevingsvurdering)

  const methods = useForm<TilbakekrevingVurdering>({
    defaultValues: behandling.tilbakekreving.vurdering || initialVurdering,
    shouldUnregister: true,
  })
  const { register, handleSubmit, watch, control, getValues, setValue, formState, reset } = methods

  useEffect(() => {
    if (formState.isDirty && Object.keys(formState.dirtyFields).length) {
      const delay = setTimeout(() => {
        lagreVurdering({ vurdering: getValues(), automatisk: true })
        reset(undefined, { keepValues: true, keepDirtyValues: true })
      }, 3000)

      return () => clearTimeout(delay)
    }
  }, [formState])

  useEffect(() => {
    if (watch().vilkaarsresultat === TilbakekrevingVilkaar.IKKE_OPPFYLT) {
      setValue('hjemmel', TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD)
    } else {
      setValue('hjemmel', getValues().rettsligGrunnlag)
    }
  }, [watch().vilkaarsresultat])

  const lagreVurdering = ({ vurdering }: { vurdering: TilbakekrevingVurdering; automatisk: boolean }) => {
    lagreVurderingRequest(
      {
        behandlingsId: behandling.id,
        vurdering: vurdering,
      },
      (behandling) => {
        if (behandling.tilbakekreving.vurdering) {
          reset(behandling.tilbakekreving.vurdering)
        }
      }
    )
  }

  const onManualSubmit = (vurdering: TilbakekrevingVurdering) => {
    lagreVurdering({ vurdering: vurdering, automatisk: false })
  }

  const vilkaarOppfylt = () =>
    watch().vilkaarsresultat &&
    [TilbakekrevingVilkaar.OPPFYLT, TilbakekrevingVilkaar.DELVIS_OPPFYLT].includes(watch().vilkaarsresultat!)

  const beloepIBehold = () =>
    watch().beloepBehold && watch().beloepBehold?.behold == TilbakekrevingBeloepBeholdSvar.BELOEP_I_BEHOLD

  return (
    <InnholdPadding>
      <VStack gap="8" style={{ width: '50em' }}>
        <Select {...register('aarsak')} label="Årsak til sak om feilutbetaling" readOnly={!redigerbar}>
          <option value="">Velg..</option>
          {Object.values(TilbakekrevingAarsak).map((aarsak) => (
            <option key={aarsak} value={aarsak}>
              {teksterTilbakekrevingAarsak[aarsak]}
            </option>
          ))}
        </Select>

        <ControlledRadioGruppe
          name="forhaandsvarsel"
          control={control}
          legend={<RadioGroupLegend label="Forhåndsvarsel" />}
          size="small"
          readOnly={!redigerbar}
          radios={
            <>
              {Object.values(TilbakekrevingVarsel).map((varsel) => (
                <Radio key={varsel} value={varsel}>
                  {teksterTilbakekrevingVarsel[varsel]}
                </Radio>
              ))}
            </>
          }
        />

        {watch().forhaandsvarsel &&
          [TilbakekrevingVarsel.MED_I_ENDRINGSBREV, TilbakekrevingVarsel.EGET_BREV].includes(
            watch().forhaandsvarsel!
          ) && (
            <ControlledDatoVelger
              name="forhaandsvarselDato"
              label="Forhåndsvarsel dato"
              control={control}
              // defaultValue={getValues().forhaandsvarselDato ?? undefined}
              readOnly={!redigerbar}
            />
          )}

        <Textarea
          {...register('beskrivelse')}
          label="Beskriv feilutbetalingen"
          readOnly={!redigerbar}
          description="Gi en kort beskrivelse av bakgrunnen for feilutbetalingen og når ble den oppdaget."
        />

        <ControlledRadioGruppe
          name="doedsbosak"
          control={control}
          legend={<RadioGroupLegend label="Dødsbosak?" />}
          size="small"
          readOnly={!redigerbar}
          radios={
            <>
              {Object.values(JaNei).map((svar) => (
                <Radio key={svar} value={svar}>
                  {JaNeiRec[svar]}
                </Radio>
              ))}
            </>
          }
        />

        <Textarea {...register('foraarsaketAv')} label="Hvem forårsaket feilutbetalingen?" readOnly={!redigerbar} />

        <ControlledRadioGruppe
          name="tilsvar.tilsvar"
          control={control}
          legend={<RadioGroupLegend label="Tilsvar til varsel om mulig tilbakekreving?" />}
          size="small"
          readOnly={!redigerbar}
          radios={
            <>
              {Object.values(JaNei).map((svar) => (
                <Radio key={svar} value={svar}>
                  {JaNeiRec[svar]}
                </Radio>
              ))}
            </>
          }
        />

        {watch().tilsvar?.tilsvar == JaNei.JA && (
          <>
            <ControlledDatoVelger
              name="tilsvar.dato"
              label="Tilsvar dato"
              control={control}
              defaultValue={getValues().tilsvar?.dato ?? undefined}
              readOnly={!redigerbar}
            />

            <Textarea {...register('tilsvar.beskrivelse')} label="Beskriv tilsvar" readOnly={!redigerbar} />
          </>
        )}

        <ControlledRadioGruppe
          name="rettsligGrunnlag"
          control={control}
          legend={<RadioGroupLegend label="Rettslig grunnlag" />}
          size="small"
          readOnly={!redigerbar}
          radios={
            <>
              {Object.values(TilbakekrevingHjemmel)
                .filter((hjemmel) => hjemmel != TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD)
                .map((hjemmel) => (
                  <Radio key={hjemmel} value={hjemmel}>
                    {teksterTilbakekrevingHjemmel[hjemmel]}
                  </Radio>
                ))}
            </>
          }
        />

        {watch().rettsligGrunnlag &&
          [
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM,
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM,
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM,
          ].includes(watch().rettsligGrunnlag!) && (
            <>
              <Textarea
                {...register('objektivtVilkaarOppfylt')}
                label="Er det objektive vilkåret oppfylt?"
                description="Foreligger det en feilutbetaling?"
                readOnly={!redigerbar}
              />

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM].includes(
                watch().rettsligGrunnlag!
              ) && (
                <Textarea
                  {...register('burdeBrukerForstaatt')}
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Forstod eller burde brukeren forstått at ubetalingen skyldes en feil?"
                  readOnly={!redigerbar}
                />
              )}

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM].includes(watch().rettsligGrunnlag!) && (
                <Textarea
                  {...register('uaktsomtForaarsaketFeilutbetaling')}
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Har brukeren uaktsomt forårsaket feilutbetalingen?"
                  readOnly={!redigerbar}
                />
              )}

              {[TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM].includes(
                watch().rettsligGrunnlag!
              ) && (
                <Textarea
                  {...register('burdeBrukerForstaattEllerUaktsomtForaarsaket')}
                  label="Er de subjektive vilkårene oppfylt?"
                  description="Forstod eller burde brukeren forstått at utbetalingen skyldtes en feil, og/eller har brukeren uaktsomt forårsaket feilutbetalingen?"
                  readOnly={!redigerbar}
                />
              )}

              <ControlledRadioGruppe
                name="vilkaarsresultat"
                control={control}
                legend=""
                size="small"
                readOnly={!redigerbar}
                radios={
                  <>
                    {Object.values(TilbakekrevingVilkaar).map((vilkaar) => (
                      <Radio key={vilkaar} value={vilkaar}>
                        {teksterTilbakekrevingVilkaar[vilkaar]}
                      </Radio>
                    ))}
                  </>
                }
              />

              {!vilkaarOppfylt() && (
                <>
                  <Textarea
                    {...register('beloepBehold.beskrivelse')}
                    label="Tilbakekreving etter folketrygdloven § 22-15 femte ledd?"
                    description="Er noe av det feilutbetalte beløpet i behold?"
                    readOnly={!redigerbar}
                  />

                  <ControlledRadioGruppe
                    name="beloepBehold.behold"
                    control={control}
                    legend=""
                    size="small"
                    readOnly={!redigerbar}
                    radios={
                      <>
                        {Object.values(TilbakekrevingBeloepBeholdSvar).map((behold) => (
                          <Radio key={behold} value={behold}>
                            {teksterTilbakekrevingBeloepBehold[behold]}
                          </Radio>
                        ))}
                      </>
                    }
                  />

                  {!beloepIBehold() && <Textarea {...register('vedtak')} label="Vedtak" readOnly={!redigerbar} />}
                </>
              )}
              {(vilkaarOppfylt() || beloepIBehold()) && (
                <>
                  <Textarea
                    {...register('reduseringAvKravet')}
                    label="Er det særlige grunner til å frafalle eller redusere kravet?"
                    description="Det legges blant annet vekt på graden av uaktsomhet hos brukeren, størrelsen på det feilutbetalte beløpet, hvor lang tid det er gått siden utbetalingen fant sted og om noe av feilen helt eller delvis kan tilskrives NAV. Kravet kan frafalles helt, eller settes til en del av det feilutbetalte beløpet. Ved utvist forsett skal krav alltid fremmes, og beløpet kan ikke settes ned."
                    readOnly={!redigerbar}
                  />

                  <Textarea
                    {...register('foreldet')}
                    label="Er noen deler av kravet foreldet?"
                    description="Det er bestemt i folketrygdloven § 22-14 første ledd at våre krav om tilbakebetaling i utgangspunktet foreldes etter foreldelsesloven. Etter foreldelsesloven § 2, jf. § 3 nr. 1 er den alminnelige foreldelsesfristen tre år. Fristen løper særskilt for hver månedsutbetaling. Vurder også om foreldelsesloven § 10 om ett års tilleggsfrist får anvendelse."
                    readOnly={!redigerbar}
                  />

                  <Textarea
                    {...register('rentevurdering')}
                    label="Skal det ilegges renter?"
                    description="Det følger av folketrygdloven § 22-17 a at det skal beregnes et rentetillegg på 10 prosent av det beløpet som kreves tilbake når brukeren har opptrådt grovt uaktsomt eller med forsett."
                    readOnly={!redigerbar}
                  />

                  <Textarea {...register('vedtak')} label="Vedtak" readOnly={!redigerbar} />

                  <Textarea
                    {...register('vurderesForPaatale')}
                    label="Skal saken vurderes for påtale?"
                    readOnly={!redigerbar}
                  />
                </>
              )}
            </>
          )}

        <div>
          <Label>Hjemmel</Label>
          <BodyLong>{watch().hjemmel ? teksterTilbakekrevingHjemmel[watch().hjemmel!] : 'Ikke satt'}</BodyLong>
        </div>

        {redigerbar && (
          <VStack gap="5">
            <Button
              variant="primary"
              size="small"
              onClick={handleSubmit(onManualSubmit)}
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

export function RadioGroupLegend({ label }: { label: string }) {
  return <div style={{ fontSize: 'large' }}>{label}</div>
}
