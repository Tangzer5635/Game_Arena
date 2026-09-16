import api from "./api";
import type { Salon } from "../types/salon";

export const createSalon = async (maxPlayers: number = 8): Promise<Salon> => {
    const response = await api.post<Salon>("/salons", null, { params: { maxPlayers } });
    return response.data;
};

export const getSalon = async (code: string): Promise<Salon> => {
    const response = await api.get<Salon>(`/salons/${code}/`);
    return response.data;
};

export const joinSalon = async (code: string): Promise<Salon> => {
    // Le backend lit l'utilisateur depuis le token JWT — pas besoin d'ID dans l'URL
    const response = await api.post<Salon>(`/salons/${code}/users/`);
    return response.data;
};

export const leaveSalon = async (code: string): Promise<void> => {
    // Idem — l'utilisateur est tiré du token côté backend
    await api.delete(`/salons/${code}/users/`);
};

export const startSalon = async (code: string, quizId: number): Promise<Salon> => {
    const response = await api.post<Salon>(
        `/salons/${code}/start/`,
        null,
        { params: { quizId } }
    );
    return response.data;
};
