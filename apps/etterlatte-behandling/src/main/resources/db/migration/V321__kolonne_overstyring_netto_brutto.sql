-- Overstyringen skjer per periode, siden en tilbakekreving kan ha perioder med og uten skatt i behold
alter table tilbakekreving drop column overstyr_netto_brutto;
alter table tilbakekrevingsperiode add column overstyr_netto_brutto text;