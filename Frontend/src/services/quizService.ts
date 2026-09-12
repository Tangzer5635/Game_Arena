import api from "./api";
import type { Quiz } from "../types/quiz";
import type { PaginatedResponse } from "../types/api";

export const getQuizzes = async (): Promise<Quiz[]> => {
    const response = await api.get<PaginatedResponse<Quiz>>("/quizs/");

    return response.data.content;
};

export const getQuizById = async (id: number): Promise<Quiz> => {
    const response = await api.get<Quiz>(`/quizs/${id}/`);

    return response.data;
};