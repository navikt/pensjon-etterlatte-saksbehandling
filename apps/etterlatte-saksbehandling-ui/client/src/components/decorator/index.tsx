import NAVSPA from "@navikt/navspa";

export interface DecoratorProps {
    appname: string; // Navn på applikasjon
    fnr?: FnrContextvalue; // Konfigurasjon av fødselsnummer-kontekst
    enhet?: EnhetContextvalue; // Konfigurasjon av enhet-kontekst
    toggles?: TogglesConfig; // Konfigurasjon av hvilke elementer som skal vises i dekoratøren
    markup?: Markup; // Ekstra innhold i dekoratøren, kan brukes om man trenger å legge en knapp innenfor dekoratøren

    useProxy?: boolean | string; // Manuell overstyring av urlene til BFFs. Gjør alle kall til relativt path hvis true, og bruker verdien som domene om satt til en string. Default: false
    accessToken?: string; // Manuell innsending av JWT, settes som Authorization-header. Om null sendes cookies vha credentials: 'include'
}

export interface TogglesConfig {
    visVeileder?: boolean; // Styrer om man skal vise informasjon om innlogget veileder
}

export interface Markup {
    etterSokefelt?: string; // Gir muligheten for sende inn egen html som blir en del av dekoratøren
}

// Fnr/Enhet-konfiguration støttet både `Controlled` og `Uncontrolled` operasjon.
// Ved bruk av `Controlled` må konsument-applikasjonen selv ta ansvar for oppdatering av `value` etter enhver `onChange`
// Dette er i motsetning til `Uncontrolled`, hvor dette håndteres av dekoratøren. Og alt konsument-applikasjonen trenger å gjøre er å følge med på `onChange`.
export interface ControlledContextvalue<T> extends BaseContextvalue<T> {
    value: string | null;
}
export interface UncontrolledContextvalue<T> extends BaseContextvalue<T> {
    initialValue: string | null;
}

export interface BaseContextvalue<T> {
    display: T;
    onChange(value: string | null): void;
    skipModal?: boolean;
    ignoreWsEvents?: boolean;
}

export type Contextvalue<T> = ControlledContextvalue<T> | UncontrolledContextvalue<T>;

export enum EnhetDisplay {
    ENHET = "ENHET",
    ENHET_VALG = "ENHET_VALG",
}

export enum FnrDisplay {
    SOKEFELT = "SOKEFELT",
}

export type EnhetContextvalue = Contextvalue<EnhetDisplay>;
export type FnrContextvalue = Contextvalue<FnrDisplay>;

const RESET_VALUE = '\u0000';
const InternflateDecorator = NAVSPA.importer<DecoratorProps>("internarbeidsflatefs");

const isDev = process.env.NODE_ENV !== "production"

export const Decorator = () => {

    const config = {
        appname: 'Etterlatte',
        useProxy: isDev ? "http://localhost:8080" : "https://etterlatte-saksbehandling.dev.intern.nav.no", 
        fnr: {
            initialValue: RESET_VALUE,
            display: FnrDisplay.SOKEFELT,
            ignoreWsEvents: true,
            onChange: (value: any) => {
                if (value) {
                    //window.location.pathname = `test/${value}`;
                }
            }
        },
        toggles: {
            visVeileder: true
        },
        enhet: {
            initialValue: "0315",
            display: EnhetDisplay.ENHET_VALG,
            onChange: (value: any) => {
                if (value) {
                    //settValgtEnhet(value);
                }
            }
        }
    };

    return (
        <div id="fullbredde">
            <InternflateDecorator {...config} />
        </div>
    );
};
