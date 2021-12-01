import { NextFunction, Request, Response } from "express";
export const authMiddleware = (req: Request, res: Response, next: NextFunction) => {
    //console.log(req.headers.authorization);
    // håndter token obo
    next();
};
