import proxy from 'express-http-proxy';
import { getOboToken } from "../middleware/getOboToken";
import { logger } from "../utils/logger";


const options: any = () => ({
    parseReqBody: false,
    
    proxyReqOptDecorator: async (options: any, req: any) => {
        console.log('headers', req.headers);   
        const oboToken = await getOboToken(req.headers.authorization);
        console.log(oboToken)
        options.headers.Authorization = `Bearer ${oboToken}`;

        /*
        logger.info(`${req.protocol?.toUpperCase()} ${req.method} ${req.path}`);

        return new Promise((resolve, reject) => {
            return exchangeToken(req.session.tokens.access_token).then(
                (accessToken) => {
                    options.headers.Authorization = `Bearer ${accessToken}`;
                    resolve(options);
                },
                (error) => {
                    logger.error("Error occured while changing request headers: ", error);
                    reject(error);
                }
            );
        });
        */
    },
    
    proxyReqPathResolver: (req: any) => {
        console.log(req.originalUrl, `${process.env.API_URL}${req.originalUrl}`);
        return req.originalUrl.replace(`https://etterlatte-saksbehandling.dev.intern.nav.no`, '')
    },
    proxyErrorHandler: (err: any, res: any, next: any) => {
        logger.error("Proxy error: ", err)
        next(err);
    }
});

export const expressProxy = proxy(`${process.env.API_URL}`, options());