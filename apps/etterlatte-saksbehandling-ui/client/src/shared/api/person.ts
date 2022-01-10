import { IApiResponse } from "./types";



const isDev = process.env.NODE_ENV !== "production"
const path = isDev ? "http://localhost:8080" : "https://etterlatte-saksbehandling.dev.intern.nav.no";


export const getPerson = async (fnr: string): Promise<IApiResponse<any>> => {
  try{
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    return {
      status: result.status,
      data: await result.json()
    }
  } catch(e) {
    console.log(e);
    return {status: 500};
  }
}