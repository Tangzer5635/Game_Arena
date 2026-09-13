import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getQuizzes } from "../services/quizService";
import { startSalon } from "../services/salonService";
import type { Quiz } from "../types/quiz";

export default function ChoixQuiz() {
    const { code } = useParams<{ code: string }>();
    const navigate = useNavigate();

    const [quizzes, setQuizzes] = useState<Quiz[]>([]);
    const [selectedQuizId, setSelectedQuizId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [starting, setStarting] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const load = async () => {
            try {
                setQuizzes(await getQuizzes());
            } catch {
                setError("Impossible de récupérer les quiz.");
            } finally {
                setLoading(false);
            }
        };
        load();
    }, []);

    const handleStart = async () => {
        if (!code || selectedQuizId === null) return;
        setStarting(true);
        setError("");
        try {
            await startSalon(code, selectedQuizId);
            navigate(`/salon/${code}/game`);
        } catch {
            setError("Impossible de lancer la partie.");
        } finally {
            setStarting(false);
        }
    };

    if (loading) return <p>Chargement des quiz...</p>;

    return (
        <div className="choix-quiz">
            <h1>Choisir un quiz</h1>
            <p>Salon : <strong>{code}</strong></p>

            {error && <p className="error">{error}</p>}

            {quizzes.length === 0 ? (
                <p>Aucun quiz disponible.</p>
            ) : (
                quizzes.map((quiz) => (
                    <div
                        key={quiz.id}
                        className={`quiz-option ${selectedQuizId === quiz.id ? "selected" : ""}`}
                        onClick={() => setSelectedQuizId(quiz.id)}
                    >
                        <h3>{quiz.titre}</h3>
                        <p>{quiz.description}</p>
                        <p style={{ fontSize: 13, color: "var(--accent)" }}>
                            {quiz.questions.length} question{quiz.questions.length > 1 ? "s" : ""}
                        </p>
                    </div>
                ))
            )}

            <button
                onClick={handleStart}
                disabled={selectedQuizId === null || starting}
                style={{ width: "100%", marginTop: 16 }}
            >
                {starting ? "Lancement..." : "🚀 Lancer la partie"}
            </button>
        </div>
    );
}
